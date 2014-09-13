/*
 * Copyright Ç©È Dec 23, 2011, Paul Woelfel, Email: frig@frig.at 
 * Created on November, 2012
 * Author : Jose Aguilar
 * Email: jose.aguilar@smail.inf.h-brs.de
 */

package at.fhstp.wificompass.trilateration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.PointF;
import android.widget.Toast;
import at.fhstp.wificompass.model.AccessPoint;
import at.fhstp.wificompass.model.BssidResult;
import at.fhstp.wificompass.model.ProjectSite;
import at.fhstp.wificompass.model.WifiScanResult;

public class WeightedCentroidTrilateration extends AccessPointTrilateration {

	protected static float g = 1.3f;

	public WeightedCentroidTrilateration(Context context, ProjectSite projectSite) {
		super(context, projectSite);
	}
	
	public WeightedCentroidTrilateration(Context context,
			ProjectSite projectSite, ProgressDialog progressDialog) {
		super(context, projectSite, progressDialog);
	}

	@Override
	public PointF calculateAccessPointPosition(AccessPoint ap) {
		Vector<MeasurementDataSet> originalData = this.measurementData.get(ap);
		
		if (originalData.size() > 3) {
			Vector<MeasurementDataSet> data = new Vector<MeasurementDataSet>();

			float sumRssi = 0;
			
			for (Iterator<MeasurementDataSet> it = originalData.iterator(); it.hasNext();) {
				MeasurementDataSet dataSet = it.next();
				
				float newRssi = (float) Math.pow(Math.pow(10, dataSet.getRssi() / 20), g);
				sumRssi += newRssi;
				
				data.add(new MeasurementDataSet(dataSet.getX(), dataSet.getY(), newRssi));
			}

			float x = 0;
			float y = 0;
			
			for (Iterator<MeasurementDataSet> itd = data.iterator(); itd.hasNext();) {
				MeasurementDataSet dataSet = itd.next();
				
				float weight = dataSet.getRssi() / sumRssi;
				x += dataSet.getX() * weight;
				y += dataSet.getY() * weight;
			}
			
			return new PointF(x, y);
		} else {
			return null;
		}
	}
	
	public PointF calculateUserPositionWiFi(WifiScanResult wr, ArrayList<AccessPoint> addedAP) {
		//Vector<MeasurementDataSet> originalData = this.measurementData.get(ap);
		
		//if (originalData.size() > 3) {
			//Vector<MeasurementDataSet> data = new Vector<MeasurementDataSet>();

			float sumRssi = 0;
		
			
//			//HashMap<String, Integer> ssids = new HashMap<String, Integer>();
//			if(wr.getBssids()!=null)
//				for (BssidResult result : wr.getBssids()) {
//					//ssids.put(result.getSsid(), (ssids.get(result.getSsid()) == null ? 1 : ssids.get(result.getSsid()) + 1));
//					// BssidResult result = it.next();
//					// Logger.d("ScanResult: " + result.toString());
//					// sb.append(result.toString());
//					// sb.append("\n");
//					//Toast.makeText(this, "ScanResult: " + result.getLevel(), Toast.LENGTH_SHORT).show();
//				
//					//float newRssi = (float) Math.pow(Math.pow(10, result.getLevel() / 20), g);
//					if(result.getLevel() > -70){
//						float newRssi = -1 * result.getLevel();
//						sumRssi += newRssi;
//					}
//				}
			
			
			if(wr.getBssids()!=null){
				for (BssidResult result : wr.getBssids()) {
				   
					if(addedAP!=null){

					for(AccessPoint tmpAP : addedAP){
					//for(int i=0; i<addedAP.size(); i++){					
					if(tmpAP.getBssid().equals(result.getBssid()) && (result.getLevel() > -85)){	
					//if(addedAP.get(i).getBssid().compareTo(result.getBssid()) == 0){
					
						    float newRssi = (float) Math.pow(Math.pow(10, result.getLevel() / 20), g);
							//float newRssi = -1 * result.getLevel();
							sumRssi += newRssi;
						
					}
						
						
					}
						
					} //del for addedAP
				 }//addedAP null check
				} // del for BSSID
			

			float x = 0;
			float y = 0;
			float xAcum = 0;
			float yAcum = 0;
			float xAP=0;
			float yAP=0;

		
			if(wr.getBssids()!=null){
				for (BssidResult result : wr.getBssids()) {
				   
					if(addedAP!=null){

					for(AccessPoint tmpAP : addedAP){
					//for(int i=0; i<addedAP.size(); i++){					
					if(tmpAP.getBssid().equals(result.getBssid()) && (result.getLevel() > -85)){	
					//if(addedAP.get(i).getBssid().compareTo(result.getBssid()) == 0){
						xAP=tmpAP.getLocation().getX();
						yAP=tmpAP.getLocation().getY();
						
						float weight = (float) (Math.pow(Math.pow(10, result.getLevel() / 20), g) ) / sumRssi;
						//float weight = (-1*result.getLevel()) / sumRssi;
						
						if(xAP!=0 && yAP!=0){
					    x = xAP * weight;
						y = yAP * weight;
						
						xAcum += x;
						yAcum += y;
					
						
						}
						
						
						}
						
					} //del for addedAP
				 }//addedAP null check
				} // del for BSSID
			
			return new PointF(xAcum, yAcum);
		} else {
			return null;
		}
	}
	

}
