/*
 * Created on November, 2012
 * Author : Jose Aguilar
 * Email: jose.aguilar@smail.inf.h-brs.de
 */

package at.fhstp.wificompass.view;

import java.sql.SQLException;

import org.metalev.multitouch.controller.MultiTouchController.PointInfo;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import at.fhstp.wificompass.model.AccessPoint;
import at.fhstp.wificompass.model.BluetoothBeacon;
import at.fhstp.wificompass.model.Location;
import at.fhstp.wificompass.model.helper.DatabaseHelper;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import de.hbrs.apin.Logger;
import de.hbrs.apin.R;

/**
 * @author  Jose Aguilar (jose.aguilar@smail.inf.h-brs.de)
 */
public class BluetoothBeaconDrawable extends MultiTouchDrawable {

	protected BitmapDrawable icon;

	/**
	 * @uml.property  name="popup"
	 * @uml.associationEnd  
	 */
	protected TextPopupDrawable popup;

	/**
	 * @uml.property  name="accessPoint"
	 * @uml.associationEnd  
	 */
	protected BluetoothBeacon accessPoint;

	/**
	 * @uml.property  name="deletePopup"
	 * @uml.associationEnd  
	 */
	protected DeleteDrawable deletePopup;

	public BluetoothBeaconDrawable(Context ctx, BluetoothBeacon accessPoint) {
		super(ctx, (RefreshableView) null);
		this.accessPoint = accessPoint;
		init();
	}

	// public AccessPointDrawable(Context ctx, MultiTouchDrawable superDrawable) {
	// super(ctx, superDrawable);
	// init();
	// }

	public BluetoothBeaconDrawable(Context ctx, MultiTouchDrawable superDrawable, BluetoothBeacon accessPoint) {
		super(ctx, superDrawable);
		this.accessPoint = accessPoint;
		init();
	}

	protected void init() {
		
        icon = (BitmapDrawable) ctx.getResources().getDrawable(R.drawable.wifi_blue_2);
	
		this.setPivot(0.5f, 0.94f);

		this.width = icon.getBitmap().getWidth();
		this.height = icon.getBitmap().getHeight();

		if (accessPoint != null && accessPoint.getLocation() != null)
			super.setRelativePosition(accessPoint.getLocation().getX(), accessPoint.getLocation().getY());

		popup = new TextPopupDrawable(ctx, this, this.getPopupText());
		popup.setWidth(250);
		popup.setActive(false);

		// allow all aps to be deleted, not only manually created ones
		deletePopup = new DeleteDrawable(ctx, this, accessPoint.getSsid() + " " + accessPoint.getBssid());
		deletePopup.setActive(false);
	}

	public Drawable getDrawable() {
		return icon;
	}

	protected String getPopupText() {
		if (accessPoint != null)
			return ctx.getString(R.string.access_point_format, accessPoint.getSsid(), accessPoint.getBssid(), accessPoint.getFrequency(),
					accessPoint.getCapabilities(), this.getRelativeX() / gridSpacingX, this.getRelativeY() / gridSpacingY);
		else
			return "";
	}

	@Override
	public boolean isScalable() {
		return false;
	}

	@Override
	public boolean isRotatable() {
		return false;
	}

	@Override
	public boolean isDragable() {
		return !accessPoint.isCalculated();
	}

	@Override
	public boolean isOnlyInSuper() {
		return true;
	}

	/**
	 * @return  the accessPoint
	 * @uml.property  name="accessPoint"
	 */
	public BluetoothBeacon getAccessPoint() {
		return accessPoint;
	}

	/**
	 * @param accessPoint  the accessPoint to set
	 * @uml.property  name="accessPoint"
	 */
	public void setAccessPoint(BluetoothBeacon accessPoint) {
		this.accessPoint = accessPoint;
		popup.setText(getPopupText());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see at.fhstp.wificompass.view.MultiTouchDrawable#onSingleTouch(org.metalev.multitouch.controller.MultiTouchController.PointInfo)
	 */
	@Override
	public boolean onSingleTouch(PointInfo pointinfo) {
		popup.setText(this.getPopupText());
		popup.setActive(!popup.isActive());
		if (deletePopup != null)
			deletePopup.setActive(popup.isActive());
		bringToFront();
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see at.fhstp.wificompass.view.MultiTouchDrawable#onDelete()
	 */
	@Override
	public void onDelete() {
		try {
			// try to delete myself from the database
			DatabaseHelper databaseHelper = OpenHelperManager.getHelper(this.ctx, DatabaseHelper.class);

			if (accessPoint.getLocation() != null)
				databaseHelper.getDao(Location.class).delete(accessPoint.getLocation());
			databaseHelper.getDao(BluetoothBeacon.class).delete(accessPoint);

			OpenHelperManager.releaseHelper();

		} catch (SQLException e) {
			Logger.w("could not delete myself from the database");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see at.fhstp.wificompass.view.MultiTouchDrawable#setRelativePosition(float, float)
	 */
	@Override
	public void setRelativePosition(float relX, float relY) {
		super.setRelativePosition(relX, relY);
		// if this is a calculated ap, we should update the db
		if (accessPoint != null && !accessPoint.isCalculated() && accessPoint.getLocation() != null && accessPoint.getLocation().getX() != relX
				&& accessPoint.getLocation().getY() != relY) {
			if (accessPoint.getLocation().getId() != 0) {
				// this ap has a id, so it was saved to the db some time
				DatabaseHelper databaseHelper = OpenHelperManager.getHelper(this.ctx, DatabaseHelper.class);

				try {
					databaseHelper.getDao(Location.class).delete(accessPoint.getLocation());
				} catch (SQLException e) {
					Logger.w("could not delete old location", e);
				}

				OpenHelperManager.releaseHelper();
			}


		}
		
		// update the location of the ap
		accessPoint.setLocation(new Location(relX, relY));
		popup.setText(getPopupText());
	}
	
	public boolean isCalculated(){
		return accessPoint.isCalculated();
	}

}
