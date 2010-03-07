package javalibusb1;

import javax.usb.*;
import javax.usb.impl.AbstractRootUsbHub;
import javax.usb.impl.DefaultUsbDeviceDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static javax.usb.UsbConst.HUB_CLASSCODE;
import static javax.usb.UsbHostManager.JAVAX_USB_PROPERTIES_FILE;

public class Libusb1UsbServices implements UsbServices {

    public static final String JAVAX_USB_LIBUSB_TRACE_PROPERTY = "javax.usb.libusb.trace";
    public static final String JAVAX_USB_LIBUSB_DEBUG_PROPERTY = "javax.usb.libusb.debug";

    UsbDeviceDescriptor rootUsbDeviceDescriptorInstance = new DefaultUsbDeviceDescriptor(
        (short) 0x0200,         // USB 2.0
        HUB_CLASSCODE,
        (byte) 0,               // See http://www.usb.org/developers/defined_class/#BaseClass09h
        (byte) 2,               // See http://www.usb.org/developers/defined_class/#BaseClass09h
        (byte) 8,               // Shouldn't really matter what we say here, any transfer will fail
        (short) 0x6666,         // Supposedly "experimental", see https://usb-ids.gowdy.us/read/UD/6666
        (short) 0,
        (short) 0x100,          // 1.0
        (byte) 0,
        (byte) 0,
        (byte) 0,
        (byte) 1);

    private libusb1 libusb;

    public Libusb1UsbServices() throws UsbException {
        libusb = libusb1.create();

        boolean trace = false;
        Integer debug_level = null;

        InputStream stream = UsbHostManager.class.getClassLoader().
            getResourceAsStream(JAVAX_USB_PROPERTIES_FILE);

        try {
            Properties properties = new Properties();

            if (stream != null) {
                properties.load(stream);
                trace = Boolean.parseBoolean(properties.getProperty(JAVAX_USB_LIBUSB_TRACE_PROPERTY, "false"));

                String s = properties.getProperty(JAVAX_USB_LIBUSB_DEBUG_PROPERTY);
                if(s != null) {
                    debug_level = Integer.parseInt(s);
                }
            }
        } catch (IOException e) {
            throw new UsbException("Error while reading configuration file.", e);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }

        trace = Boolean.getBoolean(JAVAX_USB_LIBUSB_TRACE_PROPERTY) || trace;
        libusb1.set_trace_calls(trace);

        debug_level = Integer.getInteger(JAVAX_USB_LIBUSB_DEBUG_PROPERTY, debug_level);

        if(debug_level != null){
            libusb.set_debug(debug_level);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        libusb.close();
    }

    public String getApiVersion() {
        return "1.0.1";
    }

    public String getImplDescription() {
        return "Usb for Java";
    }

    public String getImplVersion() {
        // TODO: Load from Maven artifact
        return "1.0-SNAPSHOT";
    }

    public UsbHub getRootUsbHub() {
        List<UsbDevice> devices = Arrays.asList((UsbDevice[]) libusb.get_devices());

        return new LibUsb1RootUsbHub(Collections.unmodifiableList(devices));
    }

    private class LibUsb1RootUsbHub extends AbstractRootUsbHub {
        private List<UsbDevice> usbDevices;

        public LibUsb1RootUsbHub(List<UsbDevice> usbDevices) {
            super("Virtual Root", null, null, Libusb1UsbServices.this.rootUsbDeviceDescriptorInstance);

            this.usbDevices = usbDevices;
        }

        public List<UsbDevice> getAttachedUsbDevices() {
            return usbDevices;
        }
    }
}