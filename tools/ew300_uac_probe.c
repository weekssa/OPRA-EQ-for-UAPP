#include <CoreFoundation/CoreFoundation.h>
#include <IOKit/IOKitLib.h>
#include <IOKit/IOCFPlugIn.h>
#include <IOKit/usb/IOUSBLib.h>
#include <IOKit/usb/USBSpec.h>
#include <stdio.h>
#include <string.h>

static void uac_request(IOUSBDeviceInterface **dev, unsigned char request,
                         unsigned short value, unsigned short index,
                         int length, const char *label) {
    unsigned char buffer[8] = {0};
    IOUSBDevRequest req;
    memset(&req, 0, sizeof(req));
    req.bmRequestType = 0xA1;
    req.bRequest = request;
    req.wValue = value;
    req.wIndex = index;
    req.wLength = length;
    req.pData = buffer;
    IOReturn result = (*dev)->DeviceRequest(dev, &req);
    printf("%s result=%d len=%d bytes=", label, result, req.wLenDone);
    for (int i = 0; i < length; i++) printf("%02X", buffer[i]);
    printf("\n");
}

static void hid_report(IOUSBDeviceInterface **dev, unsigned char reportId, int length, const char *label) {
    unsigned char buffer[64] = {0};
    IOUSBDevRequest req;
    memset(&req, 0, sizeof(req));
    req.bmRequestType = 0xA1;  // IN, class, interface
    req.bRequest = 0x01;       // HID GET_REPORT
    req.wValue = (0x01 << 8) | reportId;
    req.wIndex = 3;            // HID interface 3
    req.wLength = length;
    req.pData = buffer;
    IOReturn result = (*dev)->DeviceRequest(dev, &req);
    printf("%s result=%d len=%d bytes=", label, result, req.wLenDone);
    for (int i = 0; i < length; i++) printf("%02X", buffer[i]);
    printf("\n");
}


int main(void) {
    CFMutableDictionaryRef matching = IOServiceMatching(kIOUSBDeviceClassName);
    io_iterator_t iterator = 0;
    if (IOServiceGetMatchingServices(kIOMainPortDefault, matching, &iterator) != KERN_SUCCESS) {
        printf("no iterator\n");
        return 2;
    }
    io_service_t service;
    while ((service = IOIteratorNext(iterator))) {
        IOCFPlugInInterface **plugin = NULL;
        SInt32 score = 0;
        IOCreatePlugInInterfaceForService(service, kIOUSBDeviceUserClientTypeID,
                                          kIOCFPlugInInterfaceID, &plugin, &score);
        IOUSBDeviceInterface **dev = NULL;
        if (plugin) {
            (*plugin)->QueryInterface(plugin, CFUUIDGetUUIDBytes(kIOUSBDeviceInterfaceID),
                                      (LPVOID *)&dev);
        }
        if (dev) {
            UInt16 vendor = 0, product = 0;
            (*dev)->GetDeviceVendor(dev, &vendor);
            (*dev)->GetDeviceProduct(dev, &product);
            if (vendor == 0x31B2 && product == 0x0111) {
                IOReturn openResult = (*dev)->USBDeviceOpen(dev);
                printf("open=%d\n", openResult);
                IOUSBConfigurationDescriptorPtr cfg = NULL;
                IOReturn cfgResult = (*dev)->GetConfigurationDescriptorPtr(dev, 0, &cfg);
                printf("cfg=%d length=%d total=%d\n", cfgResult,
                       cfg ? cfg->bLength : 0, cfg ? cfg->wTotalLength : 0);
                if (cfg) {
                    unsigned char *p = (unsigned char *)cfg;
                    for (int i = 0; i < cfg->wTotalLength; i++) {
                        printf("%02X", p[i]);
                    }
                    printf("\n");
                }
                if (openResult == 0) {
                    unsigned char sampleRate[3] = {0};
                    IOUSBDevRequest rateRequest;
                    memset(&rateRequest, 0, sizeof(rateRequest));
                    rateRequest.bmRequestType = 0xA2;  // IN, class, endpoint
                    rateRequest.bRequest = 0x81;       // GET_CUR
                    rateRequest.wValue = 0x0100;       // SAMPLING_FREQ_CONTROL, ch 0
                    rateRequest.wIndex = 0x01;       // endpoint 0x01, interface 2
                    rateRequest.wLength = 3;
                    rateRequest.pData = sampleRate;
                    IOReturn rateResult = (*dev)->DeviceRequest(dev, &rateRequest);
                    unsigned rate = sampleRate[0] | (sampleRate[1] << 8) | (sampleRate[2] << 16);
                    printf("rate_result=%d rate=%u bytes=%02X%02X%02X\n",
                           rateResult, rate, sampleRate[0], sampleRate[1], sampleRate[2]);

                    unsigned char mute = 0;
                    IOUSBDevRequest muteGet;
                    memset(&muteGet, 0, sizeof(muteGet));
                    muteGet.bmRequestType = 0xA1;  // IN, class, interface
                    muteGet.bRequest = 0x81;       // GET_CUR
                    muteGet.wValue = 0x0100;       // MUTE_CONTROL, master
                    muteGet.wIndex = 0x0200;       // feature unit 2, AC interface 0
                    muteGet.wLength = 1;
                    muteGet.pData = &mute;
                    IOReturn muteResult = (*dev)->DeviceRequest(dev, &muteGet);
                    printf("mute_result=%d mute=%u\n", muteResult, mute);

                    hid_report(dev, 0x01, 1, "hid_01");
                    hid_report(dev, 0x4B, 10, "hid_4B");
                    hid_report(dev, 0x54, 10, "hid_54");

                    int units[] = {2, 4, 7};
                    for (int unitIndex = 0; unitIndex < 3; unitIndex++) {
                        int unit = units[unitIndex];
                        char label[48];
                        snprintf(label, sizeof(label), "unit%d_mute_get", unit);
                        uac_request(dev, 0x81, 0x0100, (unsigned short)(unit << 8), 1, label);
                        snprintf(label, sizeof(label), "unit%d_vol_get", unit);
                        uac_request(dev, 0x81, 0x0201, (unsigned short)(unit << 8), 2, label);
                        snprintf(label, sizeof(label), "unit%d_vol_min", unit);
                        uac_request(dev, 0x82, 0x0201, (unsigned short)(unit << 8), 2, label);
                        snprintf(label, sizeof(label), "unit%d_vol_max", unit);
                        uac_request(dev, 0x83, 0x0201, (unsigned short)(unit << 8), 2, label);
                        snprintf(label, sizeof(label), "unit%d_vol_res", unit);
                        uac_request(dev, 0x84, 0x0201, (unsigned short)(unit << 8), 2, label);
                    }
                }
                if (openResult == 0) (*dev)->USBDeviceClose(dev);
            }
            (*dev)->Release(dev);
        }
        if (plugin) (*plugin)->Release(plugin);
        IOObjectRelease(service);
    }
    IOObjectRelease(iterator);
    return 0;
}

