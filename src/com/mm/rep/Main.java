package com.mm.rep;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import org.openqa.selenium.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.csvreader.CsvWriter;
import org.apache.commons.lang3.StringUtils;

public class Main {
	private static final Logger logger = LogManager.getLogger(Main.class);

	private static WebDriver driver = null;
	private static GetMethod getMethod = null;
	private static Set<Cookie> bcookies = null;
	private final static String BLOGINURL = "https://passport.bilibili.com/login";
	private final static String BMAINPAGE = "https://www.bilibili.com/";

	Main() {
		BasicConfigurator.configure();
		// 初始化GetMethod,设置不变的RequestHeader
		getMethod = new GetMethod();
		getMethod.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "UTF-8");
		getMethod.addRequestHeader(":authority", "api.bilibili.com");
		getMethod.addRequestHeader(":method", "api.bilibili.com");
		getMethod.addRequestHeader(":scheme", "GET");
		getMethod.addRequestHeader(":scheme", "https");
		getMethod.addRequestHeader("accept", "*/*");
		getMethod.addRequestHeader(":scheme", "https");
		getMethod.addRequestHeader("accept-language", "zh-CN,zh;q=0.9");
		getMethod.addRequestHeader("sec-fetch-dest", "script");
		getMethod.addRequestHeader("sec-fetch-mode", "no-cors");
		getMethod.addRequestHeader("sec-fetch-site", "same-site");
		getMethod.addRequestHeader("user-agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.83 Safari/537.36");
	}

	public static String getCookie() throws InterruptedException {

		String scCookie = null;
		Scanner ip = new Scanner(System.in);
		logger.info("请输入Cookie,如果没有请按回车:");
		scCookie = ip.nextLine();

		if (scCookie.length() != 0) {
			return scCookie;
		}

		logger.info("开始扫码登录");
		// 设置驱动地址
		System.setProperty("webdriver.chrome.driver", "H:/chromedriver/chromedriver.exe");
		// 启动设置
		ChromeOptions options = new ChromeOptions();
		// 创建ChromeDriver
		driver = new ChromeDriver(options);
		// 打开Bilibili登录页面
		driver.get(BLOGINURL);
		// 等待扫码登录
		while (true) {
			if (driver.getCurrentUrl().equals(BMAINPAGE)) {
				break;
			} else {
				Thread.sleep(100);
			}
		}

		logger.info("扫码登录成功");

		// 获取cookie
		bcookies = driver.manage().getCookies();
		String cookie = StringUtils.join(bcookies, "; ");

		return cookie;
	}

	public static List<JSONObject> getFanS(String cookie, String vmid, int pn, int ps)
			throws InterruptedException, HttpException, IOException {

		HttpClient client = new HttpClient();
		// 拼接url
		StringBuffer sBuffer = new StringBuffer();
		sBuffer.append("https://api.bilibili.com/x/relation/followers?vmid=");
		sBuffer.append(vmid);
		sBuffer.append("&pn=");
		sBuffer.append(pn);
		sBuffer.append("&ps=");
		sBuffer.append(ps);
		sBuffer.append("&order=desc&jsonp=jsonp");
		getMethod.setURI(new URI(sBuffer.toString(), true));
		getMethod.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "UTF-8");
		// 设置请求头
		getMethod.addRequestHeader("cookie", cookie);
		// 发送请求
		client.executeMethod(getMethod);
		// 获取数据
		String info = new String(getMethod.getResponseBody(), "UTF-8");
		JSONObject fans = JSONObject.parseObject(info).getJSONObject("data");
		JSONArray fArray = JSONArray.parseArray(fans.getString("list"));

		return JSON.parseArray(fArray.toJSONString(), JSONObject.class);
	}

	public static void main(String[] args) throws InterruptedException, HttpException {
		logger.info("程序开始...");
		new Main();
		// 获取Cookie
		String cookie = Main.getCookie();

		CsvWriter csvWriter = new CsvWriter("C:\\Users\\computer\\Desktop\\aaa.csv", ',', Charset.forName("UTF-8"));
		String[] csvHeaders = { "mid", "粉丝名字", "粉丝签名", "粉丝头像" };
		try {
			csvWriter.writeRecord(csvHeaders);

			int pn = 1;
			boolean end = false;
			while (true) {
				for (JSONObject f : Main.getFanS(cookie, "309103931", pn, 20)) {
					if (f == null) {
						end = true;
						break;
					}
					String[] csvContent1 = { f.getString("mid"), f.getString("uname"), f.getString("sign"),
							f.getString("face") };
					System.out.println(csvContent1);
					csvWriter.writeRecord(csvContent1);
				}
				pn++;
				Thread.sleep(100);
				if (end == true) {
					break;
				}
			}
		} catch (IOException e) {
			System.out.println(e);
			e.printStackTrace();
		}

		csvWriter.close();
		driver.close();
		logger.info("程序结束");
	}

}
