package gr.uoa.di.android.helpers;

import android.Manifest.permission;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

// TODO : hash
public final class DeviceIdentifier {

	private DeviceIdentifier() {}

	/** @see http://code.google.com/p/android/issues/detail?id=10603 */
	private static final String ANDROID_ID_BUG_MSG = "The device suffers from "
		+ "the Android ID bug - its ID is the emulator ID : 9774d56d682e549c";
	private static volatile String uuid; // TODO volatile needed ?

	/**
	 * Returns a unique identifier for this device. The first (in the order the
	 * enums constants as defined in the IDs enum) non null identifier is
	 * returned or a DeviceIDException is thrown. A DeviceIDException is also
	 * thrown if ignoreBuggyAndroidID is false and the device has the Android ID
	 * bug
	 *
	 * @param ctx
	 *            an Android constant (to retrieve system services)
	 * @param ignoreBuggyAndroidID
	 *            if false, on a device with the android ID bug, the buggy
	 *            android ID is not returned instead a DeviceIDException is
	 *            thrown
	 * @return a *device* ID - null is never returned, instead a
	 *         DeviceIDException is thrown
	 * @throws DeviceIDException
	 *             if none of the enum methods manages to return a device ID
	 */
	public static String getDeviceIdentifier(Context ctx,
			boolean ignoreBuggyAndroidID) throws DeviceIDException {
		if (uuid == null) {
			synchronized (DeviceIdentifier.class) {
				if (uuid == null) {
					for (IDs id : IDs.values()) {
						try {
							uuid = id.getId(ctx);
						} catch (DeviceIDNotUniqueException e) {
							if (!ignoreBuggyAndroidID)
								throw new DeviceIDException(e);
						}
						if (uuid != null) return uuid;
					}
					throw new DeviceIDException();
				}
			}
		}
		return uuid;
	}

	private static enum IDs {
		TELEPHONY_ID {

			@Override
			String getId(Context ctx) {
				// TODO : add a SIM based mechanism ? tm.getSimSerialNumber();
				assertPermission(ctx, permission.READ_PHONE_STATE);
				final TelephonyManager tm = (TelephonyManager) ctx
						.getSystemService(Context.TELEPHONY_SERVICE);
				if (tm == null) {
					w("Telephony Manager not available");
					return null;
				}
				return tm.getDeviceId();
			}
		},
		ANDROID_ID {

			@Override
			String getId(Context ctx) throws DeviceIDException {
				final String andoidId = Secure.getString(
					ctx.getContentResolver(),
					android.provider.Settings.Secure.ANDROID_ID);
				if ("9774d56d682e549c".equals(andoidId)) {
					e(ANDROID_ID_BUG_MSG);
					throw new DeviceIDNotUniqueException();
				}
				return andoidId;
			}
		},
		WIFI_MAC {

			@Override
			String getId(Context ctx) {
				WifiManager wm = (WifiManager) ctx
						.getSystemService(Context.WIFI_SERVICE);
				if (wm == null) {
					w("Wifi Manager not available");
					return null;
				}
				assertPermission(ctx, permission.ACCESS_WIFI_STATE); // I guess
				// getMacAddress() has no java doc !!!
				return wm.getConnectionInfo().getMacAddress();
			}
		},
		BLUETOOTH_MAC {

			@Override
			String getId(Context ctx) {
				BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
				if (ba == null) {
					w("Bluetooth Adapter not available");
					return null;
				}
				assertPermission(ctx, permission.BLUETOOTH);
				return ba.getAddress();
			}
		}
		// TODO PSEUDO_ID
		// http://www.pocketmagic.net/2011/02/android-unique-device-id/
		;

		private final static String TAG = IDs.class.getSimpleName();

		abstract String getId(Context ctx) throws DeviceIDException;

		private static void w(String msg) {
			Log.w(TAG, msg);
		}

		private static void e(String msg) {
			Log.e(TAG, msg);
		}
	}

	private static void assertPermission(Context ctx, String perm) {
		final int checkPermission = ctx.getPackageManager().checkPermission(
			perm, ctx.getPackageName());
		if (checkPermission != PackageManager.PERMISSION_GRANTED) {
			throw new SecurityException("Permission " + perm + " is required");
		}
	}

	// =========================================================================
	// Exceptions
	// =========================================================================
	public static class DeviceIDException extends Exception {

		private static final long serialVersionUID = -8083699995384519417L;
		private static final String NO_ANDROID_ID = "Could not retrieve a "
			+ "device ID";

		public DeviceIDException(Throwable throwable) {
			super(NO_ANDROID_ID, throwable);
		}

		public DeviceIDException(String detailMessage) {
			super(detailMessage);
		}

		public DeviceIDException() {
			super(NO_ANDROID_ID);
		}
	}

	public static final class DeviceIDNotUniqueException extends
			DeviceIDException {

		private static final long serialVersionUID = -8940090896069484955L;

		public DeviceIDNotUniqueException() {
			super(ANDROID_ID_BUG_MSG);
		}
	}
}