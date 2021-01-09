package com.charonchui.cyberlink.util;

import org.cybergarage.upnp.Device;

public class DLNAUtil {

	private static final String MEDIARENDER = "urn:schemas-upnp-org:device:MediaRenderer:1";

	/**
	 * Check if the device is a media render device
	 * 
	 * @param device
	 * @return
	 */
	public static boolean isMediaRenderDevice(Device device) {
        return device != null
                && MEDIARENDER.equalsIgnoreCase(device.getDeviceType());
    }
}
