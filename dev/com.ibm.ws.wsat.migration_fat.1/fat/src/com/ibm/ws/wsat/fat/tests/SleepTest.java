/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class SleepTest extends WSATTest {

	private static LibertyServer server;
	private static String BASE_URL;
	private static LibertyServer server2;
	private static String BASE_URL2;
	
	private static final Duration normalStartupTime = Duration.ofSeconds(30); // Normal test machine startup time
	private static Duration meanStartupTime;

	@BeforeClass
	public static void beforeTests() throws Exception {

		server = LibertyServerFactory
				.getLibertyServer("WSATSleep");
		BASE_URL = "http://" + server.getHostname() + ":"
				+ server.getHttpDefaultPort();
		server2 = LibertyServerFactory
				.getLibertyServer("MigrationServer2");
		server2.setHttpDefaultPort(9992);
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

		DBTestBase.initWSATTest(server);
		DBTestBase.initWSATTest(server2);

		ShrinkHelper.defaultDropinApp(server, "simpleClient", "com.ibm.ws.wsat.simpleclient.client.simple");
		ShrinkHelper.defaultDropinApp(server2, "simpleServer", "com.ibm.ws.wsat.simpleserver.server");

		server.setServerStartTimeout(START_TIMEOUT);
		server2.setServerStartTimeout(START_TIMEOUT);

		meanStartupTime = FATUtils.startServers(server, server2);

		Log.info(SleepTest.class, "beforeTests", "Mean server start time: " + meanStartupTime);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(server, server2);

		DBTestBase.cleanupWSATTest(server);
		DBTestBase.cleanupWSATTest(server2);
	}

	@Test
	@Mode(TestMode.LITE)
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE094FVT() {
		callServlet("WSATRE094FVT");
	}

	@Test
	public void testWSATRE095FVT() {
		callServlet("WSATRE095FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	public void testWSATRE096FVT() {
		callServlet("WSATRE096FVT");
	}

	@Test
	@Mode(TestMode.LITE)
	public void testWSATRE097FVT() {
		callServlet("WSATRE097FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	public void testWSATRE098FVT() {
		callServlet("WSATRE098FVT");
	}

	@Test
	public void testWSATRE099FVT() {
		callServlet("WSATRE099FVT");
	}

	@Test
	@Mode(TestMode.LITE)
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	public void testWSATRE100FVT() {
		callServlet("WSATRE100FVT");
	}

	@Test
	public void testWSATRE101FVT() {
		callServlet("WSATRE101FVT");
	}

	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE102FVT() {
		callServlet("WSATRE102FVT");
	}

	@Test
	@Mode(TestMode.LITE)
	public void testWSATRE103FVT() {
		callServlet("WSATRE103FVT");
	}

	@Test
	public void testWSATRE104FVT() {
		callServlet("WSATRE104FVT");
	}

	@Test
	public void testWSATRE105FVT() {
		callServlet("WSATRE105FVT");
	}

	private void callServlet(String testMethod){
		try {
			float perf = (float)normalStartupTime.getSeconds() / (float)meanStartupTime.getSeconds();
			perf = (float) (perf > 1.0 ? 1.0 : perf);
			Log.info(SleepTest.class, "callServlet", "Perf: " + Float.toString(perf));
			
			// The perf param is an indication of how slow the test machine is
			String urlStr = BASE_URL + "/simpleClient/SimpleClientServlet"
					+ "?method=" + Integer.parseInt(testMethod.substring(6, 9))
					+ "&baseurl=" + BASE_URL2
					+ "&perf=" + perf;
			Log.info(SleepTest.class, "callServlet", "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			System.out.println(testMethod + " Result : " + result);
			assertTrue("Cannot get expected reply from server, result = '" + result + "'",
					result.contains("Test passed"));
		} catch (Exception e) {
			e.printStackTrace(System.out);
			fail("Exception happens: " + e.toString());
		}
	}
}
