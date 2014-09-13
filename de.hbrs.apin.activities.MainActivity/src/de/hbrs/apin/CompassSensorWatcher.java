///// This block uses the 4.2.2 for the Samsung Galaxy 4 API 18 
//
//
///* * Created on May 16, 2012
// * Author: Paul Woelfel
// * Email: frig@frig.at*/
// 
//package de.hbrs.apin;
//
//import java.util.Timer;
//import java.util.TimerTask;
//
//
//import android.content.Context;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;
//import android.hardware.SensorManager;
//
//
///**
// * @author Paul Woelfel (paul@woelfel.at)
// */
//public class CompassSensorWatcher implements SensorEventListener {
//
//	protected SensorManager sensorManager;
//
//	protected Sensor compass;
//	
//	//protected Sensor gyro;
//
//	protected Sensor accelerometer;
//
//	protected Context context;
//
//	float[] inR = new float[16];
//
//	float[] I = new float[16];
//
//	float[] gravity = new float[3];
//
//	float[] geomag = new float[3];
//
//	float[] orientVals = new float[3];
//
//	float azimuth = 0;
//
//	float angle = 0;
//
////	String azimuthText = "";
//
//	int minX = 0, minY = 0, maxX = 0, maxY = 0, centerX = 0, centerY = 0, width = 0, height = 0;
//
//	float l = 0.3f;
//	
//	protected CompassListener listener;
//
//	protected float lastAzimuth = 0f;
//	
//	// angular speeds from gyro
//    private float[] gyro = new float[3];
// 
//    // rotation matrix from gyro data
//    private float[] gyroMatrix = new float[9];
// 
//    // orientation angles from gyro matrix
//    private float[] gyroOrientation = new float[3];
// 
//    // magnetic field vector
//    private float[] magnet = new float[3];
// 
//    // accelerometer vector
//    private float[] accel = new float[3];
// 
//    // orientation angles from accel and magnet
//    private float[] accMagOrientation = new float[3];
// 
//    // final orientation angles from sensor fusion
//    private float[] fusedOrientation = new float[3];
// 
//    // accelerometer and magnetometer based rotation matrix
//    private float[] rotationMatrix = new float[9];
//    
//    public static final float EPSILON = 0.000000001f;
//    private static final float NS2S = 1.0f / 1000000000.0f;
//	private float timestamp;
//	private boolean initState = true;
//    
//	public static final int TIME_CONSTANT = 30;
//	public static final float FILTER_COEFFICIENT = 0.98f;
//	private Timer fuseTimer = new Timer();
//	
//	
//
//	public CompassSensorWatcher(Context context,CompassListener cl,float lowpassFilter) {
//		this.context = context;
//		this.listener=cl;
//		this.l=lowpassFilter;
//		
//	      gyroOrientation[0] = 0.0f;
//	        gyroOrientation[1] = 0.0f;
//	        gyroOrientation[2] = 0.0f;
//	 
//	        // initialise gyroMatrix with identity matrix
//	        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
//	        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
//	        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;
//	 
//	        // get sensorManager and initialise sensor listeners
//	        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
//	        initListeners();
//	        
//	        // wait for one second until gyroscope and magnetometer/accelerometer
//	        // data is initialised then scedule the complementary filter task
//	        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
//	                                      1000, TIME_CONSTANT);
//		
//
//		try {
//			sensorManager.registerListener(this, compass, SensorManager.SENSOR_DELAY_UI);
//			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
//		} catch (Exception e) {
//			Logger.e("could not register listener", e);
//		}
//	}
//
//	
///*	 * (non-Javadoc)
//	 * 
//	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)*/
//	 
//	@Override
//	public void onAccuracyChanged(Sensor sensor, int accuracy) {
//	}
//	
//	
//    public void initListeners(){
//        sensorManager.registerListener(this,
//            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//            sensorManager.SENSOR_DELAY_FASTEST);
//     
//        sensorManager.registerListener(this,
//            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
//            sensorManager.SENSOR_DELAY_FASTEST);
//     
//        sensorManager.registerListener(this,
//            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
//            sensorManager.SENSOR_DELAY_FASTEST);
//    }
//	
//
//	
///*	 * (non-Javadoc)
//	 * 
//	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)*/
//	 
//	@Override
//	public void onSensorChanged(SensorEvent event) {
//
//		// Logger.d("sensor changed "+event);
//		// we use TYPE_MAGNETIC_FIELD to get changes in the direction, but use SensorManager to get directions
//		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
//			return;
//
//		// Gets the value of the sensor that has been changed
//		switch(event.sensor.getType()) {
//	    case Sensor.TYPE_ACCELEROMETER:
//	        // copy new accelerometer data into accel array and calculate orientation
//	        System.arraycopy(event.values, 0, accel, 0, 3);
//	        calculateAccMagOrientation();
//	        break;
//	 
//	    case Sensor.TYPE_GYROSCOPE:
//	        // process gyro data
//	        gyroFunction(event);
//	        break;
//	 
//	    case Sensor.TYPE_MAGNETIC_FIELD:
//	        // copy new magnetometer data into magnet array
//	        System.arraycopy(event.values, 0, magnet, 0, 3);
//	        break;
//	    }
//
////		// If gravity and geomag have values then find rotation matrix
////		if (gravity != null && geomag != null) {
////
////			// checks that the rotation matrix is found
////			boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
////			if (success) {
////				SensorManager.getOrientation(inR, orientVals);
////
////				angle = (float) ToolBox.normalizeAngle(orientVals[0]);
////				azimuth = (float) Math.toDegrees(angle);
//
//		//accMagOrientation and gyroOrientation, fusedOrientation is the fusion between the gyroscope and Magnetometer orientation		
//		azimuth = (float) (fusedOrientation[0] * 180/Math.PI);		
//		
//		lowPassFilter();
//				
//				angle=(float) Math.toRadians(azimuth);
//
//////				azimuthText = getAzimuthLetter(azimuth) + " " + Integer.toString((int) azimuth) + "¡";
//
//				
//		angle=(float) Math.toRadians(azimuth);
//		if(listener!=null){
//					listener.onCompassChanged(azimuth,angle,getAzimuthLetter(azimuth));
//				//}
//			//}
//		}
//	}
//	
//	
//	
//	
//	public void stop(){
//		try {
//			sensorManager.unregisterListener(this);
//		} catch (Exception e) {
//			Logger.w("could not unregister listener", e);
//		}
//	}
//	
//	
//	// calculates orientation angles from accelerometer and magnetometer output
//		public void calculateAccMagOrientation() {
//		    if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
//		        SensorManager.getOrientation(rotationMatrix, accMagOrientation);
//		    }
//		}
//		
//		// This function is borrowed from the Android reference
//		// at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
//		// It calculates a rotation vector from the gyroscope angular speed values.
//	    private void getRotationVectorFromGyro(float[] gyroValues,
//	            float[] deltaRotationVector,
//	            float timeFactor)
//		{
//			float[] normValues = new float[3];
//			
//			// Calculate the angular speed of the sample
//			float omegaMagnitude =
//			(float)Math.sqrt(gyroValues[0] * gyroValues[0] +
//			gyroValues[1] * gyroValues[1] +
//			gyroValues[2] * gyroValues[2]);
//			
//			// Normalize the rotation vector if it's big enough to get the axis
//			if(omegaMagnitude > EPSILON) {
//			normValues[0] = gyroValues[0] / omegaMagnitude;
//			normValues[1] = gyroValues[1] / omegaMagnitude;
//			normValues[2] = gyroValues[2] / omegaMagnitude;
//			}
//			
//			// Integrate around this axis with the angular speed by the timestep
//			// in order to get a delta rotation from this sample over the timestep
//			// We will convert this axis-angle representation of the delta rotation
//			// into a quaternion before turning it into the rotation matrix.
//			float thetaOverTwo = omegaMagnitude * timeFactor;
//			float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
//			float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
//			deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
//			deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
//			deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
//			deltaRotationVector[3] = cosThetaOverTwo;
//		}
//		
//	    // This function performs the integration of the gyroscope data.
//	    // It writes the gyroscope based orientation into gyroOrientation.
//	    @SuppressWarnings("static-access")
//		public void gyroFunction(SensorEvent event) {
//	        // don't start until first accelerometer/magnetometer orientation has been acquired
//	        if (accMagOrientation == null)
//	            return;
//	     
//	        // initialisation of the gyroscope based rotation matrix
//	        if(initState) {
//	            float[] initMatrix = new float[9];
//	            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
//	            float[] test = new float[3];
//	            SensorManager.getOrientation(initMatrix, test);
//	            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
//	            initState = false;
//	        }
//	     
//	        // copy the new gyro values into the gyro array
//	        // convert the raw gyro data into a rotation vector
//	        float[] deltaVector = new float[4];
//	        if(timestamp != 0) {
//	            final float dT = (event.timestamp - timestamp) * NS2S;
//	        System.arraycopy(event.values, 0, gyro, 0, 3);
//	        getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
//	        }
//	     
//	        // measurement done, save current time for next interval
//	        timestamp = event.timestamp;
//	     
//	        // convert rotation vector into rotation matrix
//	        float[] deltaMatrix = new float[9];
//	        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
//	        // apply the new rotation interval on the gyroscope based rotation matrix
//	        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);
//	     
//	        // get the gyroscope based orientation from the rotation matrix
//	        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
//	    }
//	    
//	    private float[] getRotationMatrixFromOrientation(float[] o) {
//	        float[] xM = new float[9];
//	        float[] yM = new float[9];
//	        float[] zM = new float[9];
//	     
//	        float sinX = (float)Math.sin(o[1]);
//	        float cosX = (float)Math.cos(o[1]);
//	        float sinY = (float)Math.sin(o[2]);
//	        float cosY = (float)Math.cos(o[2]);
//	        float sinZ = (float)Math.sin(o[0]);
//	        float cosZ = (float)Math.cos(o[0]);
//	     
//	        // rotation about x-axis (pitch)
//	        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
//	        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
//	        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;
//	     
//	        // rotation about y-axis (roll)
//	        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
//	        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
//	        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;
//	     
//	        // rotation about z-axis (azimuth)
//	        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
//	        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
//	        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;
//	     
//	        // rotation order is y, x, z (roll, pitch, azimuth)
//	        float[] resultMatrix = matrixMultiplication(xM, yM);
//	        resultMatrix = matrixMultiplication(zM, resultMatrix);
//	        return resultMatrix;
//	    }
//	    
//	    private float[] matrixMultiplication(float[] A, float[] B) {
//	        float[] result = new float[9];
//	     
//	        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
//	        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
//	        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];
//	     
//	        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
//	        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
//	        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];
//	     
//	        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
//	        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
//	        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];
//	     
//	        return result;
//	    }
//	    
//	    class calculateFusedOrientationTask extends TimerTask {
//	        public void run() {
//	            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
//	            
//	            
//	            /* * Fix for 179° <--> -179° transition problem:
//	             * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
//	             * If so, add 360° (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360° from the result
//	             * if it is greater than 180°. This stabilizes the output in positive-to-negative-transition cases.
//	             */
//	            
//	            // azimuth
//	            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
//	            	fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
//	        		fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
//	            }
//	            else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
//	            	fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
//	            	fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
//	            }
//	            else {
//	            	fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
//	            }
//	            
//	            // pitch
//	            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
//	            	fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
//	        		fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
//	            }
//	            else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
//	            	fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
//	            	fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
//	            }
//	            else {
//	            	fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
//	            }
//	            
//	            // roll
//	            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
//	            	fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
//	        		fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
//	            }
//	            else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
//	            	fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
//	            	fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
//	            }
//	            else {
//	            	fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
//	            }
//	     
//	            // overwrite gyro matrix and orientation with fused orientation
//	            // to comensate gyro drift
//	            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
//	            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
//	            
//	            
//	            // update sensor output in GUI
//	           // mHandler.post(updateOreintationDisplayTask);
//	        }
//	    }
//
//	public String getAzimuthLetter(float azimuth) {
//		String letter = "";
//		int a = (int) azimuth;
//
//		if (a < 23 || a >= 315) {
//			letter = "N";
//		} else if (a < 45 + 23) {
//			letter = "NO";
//		} else if (a < 90 + 23) {
//			letter = "O";
//		} else if (a < 135 + 23) {
//			letter = "SO";
//		} else if (a < (180 + 23)) {
//			letter = "S";
//		} else if (a < (225 + 23)) {
//			letter = "SW";
//		} else if (a < (270 + 23)) {
//			letter = "W";
//		} else {
//			letter = "NW";
//		}
//
//		return letter;
//	}
//
//	protected void lowPassFilter() {
//		// lowpass filter
//		float dazimuth = azimuth -lastAzimuth;
//
////		// if the angle changes more than 180¡, we want to change direction and follow the shorter angle
//		if (dazimuth > 180) {
//			// change to range -180 to 0
//			dazimuth = (float) (dazimuth - 360f);
//		} else if (dazimuth < -180) {
//			// change to range 0 to 180
//			dazimuth = (float) (360f + dazimuth);
//		}
//		// lowpass filter
//		azimuth = lastAzimuth+ dazimuth*l;
//		
//		azimuth%=360;
//		
//		if(azimuth<0){
//			azimuth+=360;
//		}
//		
//		lastAzimuth=azimuth;
//		
////		lastAzimuth=azimuth=ToolBox.lowpassFilter(lastAzimuth, azimuth, l);
//		
////		oldValue + filter * (newValue - oldValue);
//
//	}
//
//}



///// This uses the 2.2 SDK for the Dell Streak API 8




/* * Created on May 16, 2012
 * Author: Paul Woelfel
 * Email: frig@frig.at*/
 
//package de.hbrs.apin;
//
//import android.content.Context;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;
//import android.hardware.SensorManager;
//
//
///**
// * @author Paul Woelfel (paul@woelfel.at)
// */
//public class CompassSensorWatcher implements SensorEventListener {
//
//	protected SensorManager sensorManager;
//
//	protected Sensor compass;
//
//	protected Sensor accelerometer;
//
//	protected Context context;
//
//	float[] inR = new float[16];
//
//	float[] I = new float[16];
//
//	float[] gravity = new float[3];
//
//	float[] geomag = new float[3];
//
//	float[] orientVals = new float[3];
//
//	float azimuth = 0;
//
//	float angle = 0;
//
////	String azimuthText = "";
//
//	int minX = 0, minY = 0, maxX = 0, maxY = 0, centerX = 0, centerY = 0, width = 0, height = 0;
//
//	float l = 0.3f;
//	
//	protected CompassListener listener;
//
//	protected float lastAzimuth = 0f;
//	
//	
//
//	public CompassSensorWatcher(Context context,CompassListener cl,float lowpassFilter) {
//		this.context = context;
//		this.listener=cl;
//		this.l=lowpassFilter;
//		
//		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
//		compass = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//
//		try {
//			sensorManager.registerListener(this, compass, SensorManager.SENSOR_DELAY_UI);
//			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
//		} catch (Exception e) {
//			Logger.e("could not register listener", e);
//		}
//	}
//
//	
///*	 * (non-Javadoc)
//	 * 
//	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)*/
//	 
//	@Override
//	public void onAccuracyChanged(Sensor sensor, int accuracy) {
//	}
//
//	
///*	 * (non-Javadoc)
//	 * 
//	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)*/
//	 
//	@Override
//	public void onSensorChanged(SensorEvent event) {
//
//		// Logger.d("sensor changed "+event);
//		// we use TYPE_MAGNETIC_FIELD to get changes in the direction, but use SensorManager to get directions
//		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
//			return;
//
//		// Gets the value of the sensor that has been changed
//		switch (event.sensor.getType()) {
//		case Sensor.TYPE_ACCELEROMETER:
//			gravity = event.values.clone();
//			break;
//		case Sensor.TYPE_MAGNETIC_FIELD:
//			geomag = event.values.clone();
//
//			break;
//		}
//
//		// If gravity and geomag have values then find rotation matrix
//		if (gravity != null && geomag != null) {
//
//			// checks that the rotation matrix is found
//			boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
//			if (success) {
//				SensorManager.getOrientation(inR, orientVals);
//
//				angle = (float) ToolBox.normalizeAngle(orientVals[0]);
//				azimuth = (float) Math.toDegrees(angle);
//
//				lowPassFilter();
//				
//				angle=(float) Math.toRadians(azimuth);
//
////				azimuthText = getAzimuthLetter(azimuth) + " " + Integer.toString((int) azimuth) + "¡";
//
//				if(listener!=null){
//					listener.onCompassChanged(azimuth,angle,getAzimuthLetter(azimuth));
//				}
//			}
//		}
//	}
//	
//	public void stop(){
//		try {
//			sensorManager.unregisterListener(this);
//		} catch (Exception e) {
//			Logger.w("could not unregister listener", e);
//		}
//	}
//
//	public String getAzimuthLetter(float azimuth) {
//		String letter = "";
//		int a = (int) azimuth;
//
//		if (a < 23 || a >= 315) {
//			letter = "N";
//		} else if (a < 45 + 23) {
//			letter = "NO";
//		} else if (a < 90 + 23) {
//			letter = "O";
//		} else if (a < 135 + 23) {
//			letter = "SO";
//		} else if (a < (180 + 23)) {
//			letter = "S";
//		} else if (a < (225 + 23)) {
//			letter = "SW";
//		} else if (a < (270 + 23)) {
//			letter = "W";
//		} else {
//			letter = "NW";
//		}
//
//		return letter;
//	}
//
//	protected void lowPassFilter() {
//		// lowpass filter
//		float dazimuth = azimuth -lastAzimuth;
//
////		// if the angle changes more than 180¡, we want to change direction and follow the shorter angle
//		if (dazimuth > 180) {
//			// change to range -180 to 0
//			dazimuth = (float) (dazimuth - 360f);
//		} else if (dazimuth < -180) {
//			// change to range 0 to 180
//			dazimuth = (float) (360f + dazimuth);
//		}
//		// lowpass filter
//		azimuth = lastAzimuth+ dazimuth*l;
//		
//		azimuth%=360;
//		
//		if(azimuth<0){
//			azimuth+=360;
//		}
//		
//		lastAzimuth=azimuth;
//		
////		lastAzimuth=azimuth=ToolBox.lowpassFilter(lastAzimuth, azimuth, l);
//		
////		oldValue + filter * (newValue - oldValue);
//
//	}
//
//}





// This block was used for the Samsung Galaxy Note 2

/*
 * Created on May 16, 2012
 * Author: Paul Woelfel
 * Email: frig@frig.at*/
 
package de.hbrs.apin;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;



/**
 * @author Paul Woelfel (paul@woelfel.at)
 */
public class CompassSensorWatcher implements SensorEventListener {

	protected SensorManager sensorManager;

	protected Sensor compass;

	protected Sensor accelerometer;
	
	protected Sensor gyro;

	protected Context context;

	float[] inR = new float[16];

	float[] I = new float[16];

	float[] gravity = new float[3];

	float[] geomag = new float[3];

	float[] orientVals = new float[3];

	float azimuth = 0;

	float angle = 0;

//	String azimuthText = "";

	int minX = 0, minY = 0, maxX = 0, maxY = 0, centerX = 0, centerY = 0, width = 0, height = 0;

	float l = 0.3f;
	
	protected CompassListener listener;

	protected float lastAzimuth = 0f;
	
	

	public CompassSensorWatcher(Context context,CompassListener cl,float lowpassFilter) {
		this.context = context;
		this.listener=cl;
		this.l=lowpassFilter;
		
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	//	compass = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD); //Android 2.2
		compass = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION); //Android 4.2
		
		gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		try {
			sensorManager.registerListener(this, compass, SensorManager.SENSOR_DELAY_UI);
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
		} catch (Exception e) {
			Logger.e("could not register listener", e);
		}
	}

	
//	 * (non-Javadoc)
//	 * 
//	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	
//	 * (non-Javadoc)
//	 * 
//	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 
	@Override
	public void onSensorChanged(SensorEvent event) {

		// Logger.d("sensor changed "+event);
		// we use TYPE_MAGNETIC_FIELD to get changes in the direction, but use SensorManager to get directions
		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
			return;

		// Gets the value of the sensor that has been changed
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			gravity = event.values.clone();
			break;
		case Sensor.TYPE_ORIENTATION:
			 azimuth = event.values[0]; //Android 4.2
				
//    	case Sensor.TYPE_MAGNETIC_FIELD:
//			geomag = event.values.clone();	 //Android 2.2
		   

			break;
		case Sensor.TYPE_GYROSCOPE:
			//	geomag = event.values.clone();
			//azimuth = event.values[0];

				break;
			
		}

		// If gravity and geomag have values then find rotation matrix // Android 2.2
//		if (gravity != null && geomag != null) {
//
//			// checks that the rotation matrix is found
//			boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
//			if (success) {
//				SensorManager.getOrientation(inR, orientVals);
//
//				angle = (float) ToolBox.normalizeAngle(orientVals[0]);
//				azimuth = (float) Math.toDegrees(angle); 
				
		//Android 2.2
				 
			

				lowPassFilter();
				
				angle=(float) Math.toRadians(azimuth);

//				azimuthText = getAzimuthLetter(azimuth) + " " + Integer.toString((int) azimuth) + "Â°";

				if(listener!=null){
					listener.onCompassChanged(azimuth,angle,getAzimuthLetter(azimuth));
				//} // Android 2.2
			//}  //Android 2.2
		}
	}
	
	public void stop(){
		try {
			sensorManager.unregisterListener(this);
		} catch (Exception e) {
			Logger.w("could not unregister listener", e);
		}
	}

	public String getAzimuthLetter(float azimuth) {
		String letter = "";
		int a = (int) azimuth;

		if (a < 23 || a >= 315) {
			letter = "N";
		} else if (a < 45 + 23) {
			letter = "NO";
		} else if (a < 90 + 23) {
			letter = "O";
		} else if (a < 135 + 23) {
			letter = "SO";
		} else if (a < (180 + 23)) {
			letter = "S";
		} else if (a < (225 + 23)) {
			letter = "SW";
		} else if (a < (270 + 23)) {
			letter = "W";
		} else {
			letter = "NW";
		}

		return letter;
	}

	protected void lowPassFilter() {
		// lowpass filter
		float dazimuth = azimuth -lastAzimuth;

//		// if the angle changes more than 180Â°, we want to change direction and follow the shorter angle
		if (dazimuth > 180) {
			// change to range -180 to 0
			dazimuth = (float) (dazimuth - 360f);
		} else if (dazimuth < -180) {
			// change to range 0 to 180
			dazimuth = (float) (360f + dazimuth);
		}
		// lowpass filter
		azimuth = lastAzimuth+ dazimuth*l;
		
		azimuth%=360;
		
		if(azimuth<0){
			azimuth+=360;
		}
		
		lastAzimuth=azimuth;
		
//		lastAzimuth=azimuth=ToolBox.lowpassFilter(lastAzimuth, azimuth, l);
		
//		oldValue + filter * (newValue - oldValue);

	}

}
