#include <CoreFoundation/CoreFoundation.h>
#include <IOKit/IOKitLib.h>
#include <IOKit/IOCFPlugIn.h>
#include <IOKit/usb/IOUSBLib.h>
#include <IOKit/usb/USBSpec.h>
#include <stdio.h>
#include <string.h>

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

