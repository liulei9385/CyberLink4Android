package com.charonchui.cyberlink.engine;

import java.util.ArrayList;
import java.util.List;

import org.cybergarage.upnp.Device;

import com.charon.cyberlink.util.DLNAUtil;
import com.charon.cyberlink.util.LogUtil;

public class DLNAContainer {
	private List<Device> mDevices;
	private Device mSelectedDevice;
	private DeviceChangeListener mDeviceChangeListener;
	private static final DLNAContainer mDLNAContainer = new DLNAContainer();
	private static final String TAG = "DLNAContainer";

	private DLNAContainer() {
		mDevices = new ArrayList<Device>();
	}

	public static DLNAContainer getInstance() {
		return mDLNAContainer;
	}

	public synchronized void addDevice(Device d) {
		if (!DLNAUtil.isMediaRenderDevice(d))
			return;
		int size = mDevices.size();
		for (int i = 0; i < size; i++) {
			String udnString = mDevices.get(i).getUDN();
			if (d.getUDN().equalsIgnoreCase(udnString)) {
				return;
			}
		}
		
		mDevices.add(d);
		LogUtil.d(TAG, "Devices add a device" + d.getDeviceType());
		if (mDeviceChangeListener != null) {
			mDeviceChangeListener.onDeviceChange(d);
		}
	}

	public synchronized void removeDevice(Device d) {
		if (!DLNAUtil.isMediaRenderDevice(d)) {
			return;
		}
		int size = mDevices.size();
		for (int i = 0; i < size; i++) {
			String udnString = mDevices.get(i).getUDN();
			if (d.getUDN().equalsIgnoreCase(udnString)) {
				Device device = mDevices.remove(i);
				LogUtil.d(TAG, "Devices remove a device");

				boolean ret = false;
				if (mSelectedDevice != null) {
					ret = mSelectedDevice.getUDN().equalsIgnoreCase(
							device.getUDN());
				}
				if (ret) {
					mSelectedDevice = null;
				}
				if (mDeviceChangeListener != null) {
					mDeviceChangeListener.onDeviceChange(d);
				}
				break;
			}
		}
	}

	public synchronized void clear() {
		if (mDevices != null) {
			mDevices.clear();
			mSelectedDevice = null;
		}
	}

	public Device getSelectedDevice() {
		return mSelectedDevice;
	}

	public void setSelectedDevice(Device mSelectedDevice) {
		this.mSelectedDevice = mSelectedDevice;
	}

	public void setDeviceChangeListener(
			DeviceChangeListener deviceChangeListener) {
		mDeviceChangeListener = deviceChangeListener;
	}

	public List<Device> getDevices() {
		return mDevices;
	}

	public interface DeviceChangeListener {
		void onDeviceChange(Device device);
	}

}
