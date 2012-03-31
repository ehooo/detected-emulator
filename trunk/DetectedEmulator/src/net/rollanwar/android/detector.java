package net.rollanwar.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

public class detector extends Activity implements OnClickListener, SensorEventListener, Runnable {
	private static final int CLEAR = 1;
	private static final int REMOVE = 0;
	public static final String LOG_TAG = "DETECT_EMU";
	protected static ProgressDialog pd;
	private static boolean sensor_detect = false;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		View boton = findViewById(R.id.Check);
		boton.setOnClickListener(this);
	}
	@Override
	public void onClick(View v) {
		if (v.getId() != R.id.Check)
			return;
		//pd = ProgressDialog.show(this, "Checking", "Try if you run on Emulator", true, false);
		new Thread(this).start();
	}
	private void startVibrator(){
		sensor_detect = false;
		final detector self = this;
		new Thread(){
			@Override
			public void run() {
				final SensorManager sensor = (SensorManager) self.getSystemService(SENSOR_SERVICE);
				for (Sensor sen : sensor.getSensorList(Sensor.TYPE_ALL)) {
					sensor.registerListener(self, sen, SensorManager.SENSOR_DELAY_FASTEST );
				}
				final Vibrator vibrator = (Vibrator) self.getSystemService(VIBRATOR_SERVICE);
				vibrator.vibrate(60 * 1000);
			}
		}.start();
	}
	private void endVibrator() {
		final SensorManager sensor = (SensorManager) getSystemService(SENSOR_SERVICE);
		for (Sensor sen : sensor.getSensorList(Sensor.TYPE_ALL)) {
			sensor.unregisterListener( this, sen );
		}
		final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		vibrator.cancel();
	}
	@Override
	public void run() {
		startVibrator();
		handler.sendEmptyMessage(CLEAR);
		Message msg = new Message();
		msg.what = R.id.Build;
		if(is_Emulated_build())
			msg.arg1 = R.string.emu;
		else
			msg.arg1 = R.string.real;
		handler.sendMessage(msg);

		msg = new Message();
		msg.what = (R.id.file);
		if(is_Emulated_file())
			msg.arg1 = R.string.emu;
		else
			msg.arg1 = R.string.real;
		handler.sendMessage(msg);

		msg = new Message();
		msg.what = (R.id.Properties);
		if(!is_Real_properties())
			msg.arg1 = R.string.emu;
		else
			msg.arg1 = R.string.real;
		handler.sendMessage(msg);

		msg = new Message();
		msg.what = (R.id.SecSet);
		if(isEmulated(getContentResolver()))
			msg.arg1 = R.string.emu;
		else
			msg.arg1 = R.string.real;
		handler.sendMessage(msg);

		msg = new Message();
		msg.what = (R.id.Network);
		if(is_Emulated_network())
			msg.arg1 = R.string.emu;
		else
			msg.arg1 = R.string.real;
		handler.sendMessage(msg);

		if(Build.VERSION.SDK_INT < 12){
			msg = new Message();
			msg.what = (R.id.Log);
			if(is_Emulated_log())
				msg.arg1 = R.string.emu;
			else
				msg.arg1 = R.string.real;
			handler.sendMessage(msg);
		}

		msg = new Message();
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		msg.what = (R.id.LocationManager);
		if(isEmulated(locationManager))
			msg.arg1 = R.string.emu;
		else
			msg.arg1 = R.string.real;
		handler.sendMessage(msg);

		msg = new Message();
		msg.what = (R.id.Vibrator);
		if(!sensor_detect)
			msg.arg1 = R.string.emu;
		else
			msg.arg1 = R.string.real;
		handler.sendMessage(msg);
		//********PERMISOS de READ_PHONE_STATE
		msg = new Message();
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		msg.what = (R.id.TelephonyManager);
		if(isEmulated(telephonyManager))
			msg.arg1 = R.string.emu;
		else
			msg.arg1 = R.string.real;
		handler.sendMessage(msg);
		//********FIN permisos de READ_PHONE_STATE

		handler.sendEmptyMessage(REMOVE);
	}
	private void sendInfo(final String txt){
		Message msg = new Message();
		msg.what = R.id.infoExtra;
		msg.obj = ">>>"+txt+"\n";
		handler.sendMessage(msg);
	}
	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == REMOVE){
				if(sensor_detect == false){
					Log.d(LOG_TAG, "finalizando vibrador");
					endVibrator();
				}if(pd!=null){
					if(pd.isShowing())
						pd.dismiss();
					pd = null;
				}
			}else if(msg.what == CLEAR){
				final EditText txt = (EditText) findViewById(R.id.infoExtra);
				txt.setText("");
			}else if(msg.what == R.id.infoExtra && msg.obj instanceof String){
				final EditText txt = (EditText) findViewById(msg.what);
				txt.append((String) msg.obj);
			}else{
				String who = "";
				switch (msg.what) {
					case R.id.Build:
						who = "Build";
						break;
					case R.id.LocationManager:
						who = "LocationManager";
						break;
					case R.id.SecSet:
						who = "ContentResolver";
						break;
					case R.id.Log:
						who = "Log";
						break;
					case R.id.Network:
						who = "Network";
						break;
					case R.id.Properties:
						who = "Properties";
						break;
					case R.id.TelephonyManager:
						who = "TelephonyManager";
						break;
					case R.id.Vibrator:
						who = "Vibrator";
						break;
				}
				final TextView txt = (TextView) findViewById(msg.what);
				if(msg.arg1 == R.string.emu){
					Log.d(LOG_TAG, "EMULADOR "+who);
					txt.setTextColor(Color.RED);
				}else if(msg.arg1 == R.string.real)
					txt.setTextColor(Color.GREEN);
				txt.setText(msg.arg1);
			}
		}
	};

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.d(LOG_TAG, "accuracy: "+String.valueOf(accuracy));
	}
	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event == null) return;
		final String sensorName = event.sensor.getName();
		float[][] black_list = {{0,0,0},{1,0,0},{0,(float) 9.77622,(float) 0.813417}};
		for(int i=0; i<black_list.length && event.values.length==3; i++){
			if(	event.values[0] == black_list[i][0] &&
				event.values[1] == black_list[i][1] &&
				event.values[2] == black_list[i][2]
				) return;
		}
		endVibrator();
		sensor_detect = true;
		sendInfo("Sensor Name: " + sensorName);
	}

	public final boolean isEmulated(final LocationManager locationManager){
		Log.d(LOG_TAG, "Check LocationManager");
		try{
			List<String> providers = locationManager.getAllProviders();
			for (String provider: providers) {
				sendInfo("Location Provider: "+provider);
			}
			final Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if(!loc.hasAccuracy())
				return true;
		}catch (Exception e) {
			return true;
		}
		return false;
	}

	public final boolean isEmulated(final TelephonyManager telephonyManager){
		Log.d(LOG_TAG, "Check TelephonyManager");
		boolean ret = false;
		final CellLocation celda = telephonyManager.getCellLocation();
		if(celda != null){
			if (celda instanceof GsmCellLocation){
				GsmCellLocation gsm = (GsmCellLocation) celda;
				if(!(gsm.getCid() == -1 && gsm.getLac() == -1))
					ret = false;
			}//Si es un terminal que soporte CdmaCellLocation
		}
		String subid = telephonyManager.getSubscriberId();
		sendInfo("Subcriber: "+subid);
		if(subid==null || subid.endsWith("0000000000"))
			ret = true;

		String id = telephonyManager.getDeviceId();
		int emu = 0;
		sendInfo("IMEI: "+id);

		if(id!=null)
			for (char cero : id.toCharArray()) {
				emu++;
				if('0' != cero) break;
			}
		if(id==null || emu == id.length())
			ret = true;//El IMEI del emu es todo 0s o Null en kernel nuevos

		id = telephonyManager.getDeviceSoftwareVersion();
		if(id == null)
			ret = true;
		else
			sendInfo("SW Ver: "+id);

		id = telephonyManager.getSimOperatorName();
		if("Android".equals(id))
			ret = true;
		else
			sendInfo("Sim. Op: "+id);

		return ret;
	}

	public final boolean isEmulated(ContentResolver contentResolver){
		Log.d(LOG_TAG, "Check ContentResolver");
		String id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
		if(id != null){
			sendInfo("ANDROID_ID: "+id);
			if(	 Build.VERSION.SDK_INT <= 7 || Build.VERSION.SDK_INT  == 9 ||
				(Build.VERSION.SDK_INT == 8  && !"9774d56d682e549c".equals(id)) ||
				(Build.VERSION.SDK_INT == 10 && !"c35a317c95028cfb".equals(id)) ||
				(Build.VERSION.SDK_INT == 11 && !( "56e56ca1d75e518f".equals(id) ||
						"e711b8f560fe1a67".equals(id) ) ) ||
				(Build.VERSION.SDK_INT == 12 && !( "30b24bd58e3d2155".equals(id) ||
						"c1eaeda4263f23cd".equals(id) ) ) ||
				(Build.VERSION.SDK_INT == 13 && !( "78744d4c2bca626f".equals(id) ||
						"aab554cb55d2cceb".equals(id) ) ) ||
				(Build.VERSION.SDK_INT == 14 && !"5f7393dce4fdb9d6".equals(id))||
				(Build.VERSION.SDK_INT == 15 && !( "c60fc0ed4ae1f3c".equals(id) ||
						"37f107f15c3551a".equals(id) ) )
				){
					return false;
			}
		}else
			sendInfo("ANDROID_ID: Null");
		return true;
	}

	public final boolean is_Real_properties(){
		Log.d(LOG_TAG, "Check System.getProperties()");
		Properties prop = System.getProperties();
		final String sep = prop.getProperty("path.separator");
		final StringBuffer props = new StringBuffer("System.getProperties: (");
		props.append(prop.size());
		props.append(")");
		for (Object key : prop.keySet()) {
			String skey = null;
			if(key instanceof String){
				skey = (String) key;
				if("java.boot.class.path".equals(skey))
					props.append("\n\t"+skey+"="+prop.getProperty(skey).replace(sep, "\n\t\t"));
				else
					props.append("\n\t"+skey+"="+prop.getProperty(skey));
			}
		}
		sendInfo(props.toString());
		boolean ret = false;
		if(prop.containsKey("http.agent") && !prop.getProperty("http.agent").contains("sdk Build"))
			ret = true;//*
		if (Build.VERSION.SDK_INT == 4 && prop.size() != 37)//37 en <1.6
			ret = true;
		else if (Build.VERSION.SDK_INT >= 7 && Build.VERSION.SDK_INT < 14 && prop.size() != 39)//39 en 2.1 y 2.2
			ret = true;
		else if (Build.VERSION.SDK_INT == 14 && prop.size() != 40)//40 en 4.0
			ret = true;//*/
		return ret;
	}

	public final boolean is_Emulated_build(){
		Log.d(LOG_TAG, "Check Build");
		boolean ret = false;
		//Usando Build
		if(Build.MODEL.equals("sdk") &&
		   Build.PRODUCT.equals("sdk") &&
		   Build.TAGS.contains("test") &&
		   (Build.HOST.contains("test") || Build.HOST.endsWith("google.com")) &&
		   Build.USER.equals("android-build") &&
		   Build.DEVICE.equals("generic") &&
		   Build.BOARD.equals("unknown") &&
		   Build.MANUFACTURER.equals("unknown"))
			ret = true;

		sendInfo("Build.MODEL: "+Build.MODEL);
		sendInfo("Build.PRODUCT: "+Build.PRODUCT);
		sendInfo("Build.TAGS: "+Build.TAGS);
		sendInfo("Build.HOST: "+Build.HOST);
		sendInfo("Build.USER: "+Build.USER);
		sendInfo("Build.DEVICE: "+Build.DEVICE);
		sendInfo("Build.BOARD: "+Build.BOARD);//*
		sendInfo("Build.MANUFACTURER: "+Build.MANUFACTURER);//*/

		return ret;
	}

	private static final class localServ extends Thread{
		private ServerSocket sock = null;
		protected InetAddress connected = null;
		protected boolean accept = false;

		public localServ(int port) throws IOException {
			sock = new ServerSocket(port);
			if(sock!=null)
				this.start();
			else
				throw new IOException("Can't create the ServerSocket");
		}

		@Override
		public void run() {
			try {
				Socket s = sock.accept();
				Log.d(LOG_TAG, "Conexion aceptada");
				accept = true;
				if (s.isConnected()){
					connected = s.getInetAddress();
					s.close();
				}
				sock.close();
			} catch (IOException e) {
				Log.d(LOG_TAG, "Error al conectar con el puerto de escucha");
			}
		}
	}
	public final boolean is_Emulated_network() {
		Log.d(LOG_TAG, "Check Network");
		boolean ret = is_Emulated_network_dns();
		final int port = 1025 + new Random().nextInt(64000);
		localServ local = null;
		InetAddress localIP = null;
		Socket sock = new Socket();
		try{
			localIP = InetAddress.getByAddress(new byte[]{10,0,2,15});
			local = new localServ(port);
			InetSocketAddress conector = new InetSocketAddress(localIP,port);
			sock.connect(conector, 1);
			sendInfo("Local Addr: "+String.valueOf(sock.getLocalAddress()));
			if(localIP.equals(sock.getLocalAddress()))
				ret = true;
			sendInfo("Conexion Addr: "+String.valueOf(local.connected));
			if (localIP.equals(local.connected))
				ret = true;
		} catch (UnknownHostException e) {
			sendInfo("Conexion Addr: UnknowHost");
		} catch (IOException e) {
			if (sock != null && localIP!=null && local!=null){
				try{
					sendInfo("Local Addr: "+sock.getLocalAddress().toString());
					if(localIP.equals(sock.getLocalAddress()))
						ret = true;
				}catch (Exception ex) {
					Log.d(LOG_TAG, "Error:"+ex.getMessage());
				}
				sendInfo("Conexion Addr: "+String.valueOf(local.connected));
				if (local.accept || localIP.equals(local.connected))
					ret = true;
			} else {
				sendInfo("Conexion Addr: IO Error");
				e.printStackTrace();
			}
		} finally{
			if(sock != null)
				try {
					sock.close();
				} catch (IOException e) {}			
		}
		return ret;
	}
	public final boolean is_Emulated_network_dns(){
		boolean ret = false;
		InetSocketAddress[] dnsIP = new InetSocketAddress[4];
		try {
			dnsIP[0] = new InetSocketAddress(InetAddress.getByAddress(new byte[]{10,0,2,3}), 53);
			dnsIP[1] = new InetSocketAddress(InetAddress.getByAddress(new byte[]{10,0,2,4}), 53);
			dnsIP[2] = new InetSocketAddress(InetAddress.getByAddress(new byte[]{10,0,2,5}), 53);
			dnsIP[3] = new InetSocketAddress(InetAddress.getByAddress(new byte[]{10,0,2,6}), 53);
		} catch (UnknownHostException e1) {
			Log.d(LOG_TAG, "DNS unknow");
		}
		Socket sock = null;
		for (InetSocketAddress dns : dnsIP) {
			try{
				sock = new Socket();
				sock.connect(dns, 1);
				ret = true;
				Log.d(LOG_TAG, "DNS server on:"+dns.toString());
				sendInfo("DNS server on:"+dns.toString());
			}catch (Exception e) {
				Log.d(LOG_TAG, "DNS error:"+e.getMessage());
			}finally{
				if (sock!=null && sock.isClosed())
					try {
						sock.close();
					} catch (IOException e) {}
			}
		}
		return ret;
	}

	public final boolean is_Emulated_file() {
		Log.d(LOG_TAG, "Check Files");
		final File f = new File("/");
		File ficheros[] = f.listFiles();
		if(ficheros!= null){
			final StringBuffer files = new StringBuffer("Files in '/'(");
			files.append(ficheros.length);
			files.append("):");
			for (File file : ficheros) {
				files.append("\n\t"+file.getName());
			}
			sendInfo(files.toString());
			if(Build.VERSION.SDK_INT == 4 && ficheros.length == 15)
				return true;
			if(Build.VERSION.SDK_INT == 7  && ficheros.length == 17)
				return true;
			if(Build.VERSION.SDK_INT == 8  && ficheros.length == 18)
				return true;
			if(Build.VERSION.SDK_INT >= 9  && ficheros.length == 21)
				return true;
		}
		return false;
	}

	public final boolean is_Emulated_log(){
		Log.d(LOG_TAG, "Check Logs");
		try{
			ArrayList<String> commandLine = new ArrayList<String>(2);
			commandLine.add("logcat");
			commandLine.add("-d");

			Process process = Runtime.getRuntime().exec(commandLine.toArray(new String[2]));
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			
			String line;
			while ((line = bufferedReader.readLine()) != null)
				if (line.contains("qemud")){
					sendInfo("Find 'qemud' in Logs: >"+line);
					process.destroy();
					return true;
				}
			process.destroy();
		} 
		catch (IOException e){}
		return false;
	}

	public final boolean is_Emulated_Smali(){
		try{
			Object a = new Object();
			Log.e("TAG", String.valueOf(a));
		}catch(Exception e){
			Log.e("TAG",e.getMessage());
		}
		return false;
	}
}

