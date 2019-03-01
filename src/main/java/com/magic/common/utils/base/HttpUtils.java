package com.magic.common.utils.base;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * http协议工具
 *
 * @author wuhoujian
 *
 * @date 2019/3/1
 */
public class HttpUtils {
	public final static int CONNECT_TIMEOUT = 600000;

	public final static int READ_TIMEOUT = 600000;

	/** 编码格式 utf-8 gbk */
	public static final String ENCODING_UTF8 = "UTF-8";
	public static final String ENCODING_GBK = "GBK";

	/** http请求参数 */
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String CONTENG_TYPE_FORM = "application/x-www-form-urlencoded";
	public static final String CONTENG_TYPE_JSON = "application/json";

	/**
	 * 设置连接超时时间(单位毫秒)／设置读数据超时时间(单位毫秒)
	 */
	public final static RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(READ_TIMEOUT)
			.setConnectTimeout(CONNECT_TIMEOUT).build();

	/**
	 * 发起https请求并获取结果 采用jdk本身的API实现
	 *
	 * @param requestUrl
	 *            请求地址
	 * @param requestMethod
	 *            请求方式（GET、POST）
	 * @param data
	 *            提交的数据
	 * @return JSONObject(通过JSONObject.get(key)的方式获取json对象的属性值)
	 */

	public static JSONObject httpRequest(String requestUrl, String requestMethod, String data) {
		JSONObject jsonObject = null;
		StringBuffer buffer = new StringBuffer();

		try {
			// 创建SSLContext对象，并使用我们指定的信任管理器初始化
			TrustManager[] tm = { new MyX509TrustManager() };
			SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
			sslContext.init(null, tm, new java.security.SecureRandom());
			// 从上述SSLContext对象中得到SSLSocketFactory对象
			SSLSocketFactory ssf = sslContext.getSocketFactory();

			URL url = new URL(requestUrl);
			HttpsURLConnection httpUrlConn = (HttpsURLConnection) url.openConnection();
			httpUrlConn.setSSLSocketFactory(ssf);

			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			// 设置请求方式（GET/POST）
			httpUrlConn.setRequestMethod(requestMethod);

			if ("GET".equalsIgnoreCase(requestMethod))
				httpUrlConn.connect();

			// 当有数据需要提交时
			if (null != data) {
				OutputStream outputStream = httpUrlConn.getOutputStream();
				// 注意编码格式，防止中文乱码
				outputStream.write(data.getBytes("UTF-8"));
				outputStream.close();
				outputStream = null; // ???
			}

			// 将返回的输入流转换成字符串
			InputStream inputStream = httpUrlConn.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String str = null;
			while ((str = bufferedReader.readLine()) != null) {
				buffer.append(str);
			}

			// 释放资源
			bufferedReader.close();
			inputStreamReader.close();
			inputStream.close();
			inputStream = null;
			httpUrlConn.disconnect();
			jsonObject = JSONObject.parseObject(buffer.toString());
		}

		catch (ConnectException ce) {
			//log.error("Weixin server connection timed out.");
		}

		catch (Exception e) {
			//log.error("https request error:{}", e);
		}

		return jsonObject;
	}

	/**
	 * POST请求 公共方法
	 *
	 * @param url
	 *            URL
	 * @param requestEntity
	 *            请求参数
	 * @return 返回结果
	 */
	public static String post(String url, String requestEntity, String contentTypeValue) {
		String result = null;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpPost httpPost = new HttpPost(url);
			//log.info("post request url:[{}]", url);
			//log.info("requestEntity:[{}]", requestEntity);
			//log.info("contentTypeValue:[{}]", contentTypeValue);
			httpPost.setConfig(requestConfig);
			httpPost.setEntity(new StringEntity(requestEntity, ENCODING_UTF8));
			httpPost.addHeader(CONTENT_TYPE, contentTypeValue);
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			result = EntityUtils.toString(httpEntity, ENCODING_UTF8);
			//log.info("response:[{}]", result);
			EntityUtils.consume(httpEntity);
		} catch (Exception e) {
			//log.error("http 请求异常", e);
			return result;
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				//log.error("http 请求关闭异常", e);
			}
		}
		return result;
	}

	/**
	 * POST请求 公共方法
	 *
	 * @param url
	 *            URL
	 * @param contentTypeValue
	 *            请求参数
	 * @return 返回结果
	 */
	public static String post(String url, Map<String, String> mapParam, String contentTypeValue) {
		String result = null;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpPost httpPost = new HttpPost(url);
			//log.info("request(url:" + url + "):" + mapParam != null ? mapParam.toString() : "");

			if (mapParam != null && mapParam.size() > 0) {
				List<NameValuePair> pairs = new ArrayList<NameValuePair>();
				for (Map.Entry<String, String> entry : mapParam.entrySet()) {
					// pairs.add(new BasicNameValuePair(entry.getKey(),
					// entry.getValue()));

					if (entry.getKey().equals("access_token")) {
						pairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
					} else {
						pairs.add(new BasicNameValuePair("\"" + entry.getKey() + "\"", "\"" + entry.getValue() + "\""));
					}
				}
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, ENCODING_UTF8);
				httpPost.setEntity(entity);
			}

			httpPost.setConfig(requestConfig);
			httpPost.addHeader(CONTENT_TYPE, contentTypeValue);
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			result = EntityUtils.toString(httpEntity, ENCODING_UTF8);
			//log.info("respnse:" + result);
			EntityUtils.consume(httpEntity);
		} catch (Exception e) {
			//log.error("http 请求异常", e);
			return result;
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				//log.error("http 请求关闭异常", e);
			}
		}
		return result;
	}

	public static String postAdvance(String url, Map<String, String> mapParam, String contentTypeValue) {
		String result = null;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			StringBuilder sb = new StringBuilder(url + "?");

			//log.info("request(url:" + url + "):" + mapParam != null ? mapParam.toString() : "");

			if (mapParam != null && mapParam.size() > 0)
				for (Map.Entry<String, String> entry : mapParam.entrySet())
					sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");

			sb.deleteCharAt(sb.length() - 1);
			//log.info("url路径是：" + sb.toString());

			HttpPost httpPost = new HttpPost(sb.toString());
			httpPost.setConfig(requestConfig);
			httpPost.addHeader(CONTENT_TYPE, contentTypeValue);
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			result = EntityUtils.toString(httpEntity, ENCODING_UTF8);
			//log.info("respnse:" + result);
			EntityUtils.consume(httpEntity);
		} catch (Exception e) {
			//log.error("http 请求异常", e);
			return result;
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				//log.error("http 请求关闭异常", e);
			}
		}
		return result;
	}

	/**
	 * GET请求 公共方法
	 *
	 * @param url
	 *            URL
	 * @param parameterMap
	 *            请求参数
	 * @return 返回结果
	 */
	public static String get(String url, Map<String, String> parameterMap) {
		String result = null;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			if (parameterMap != null) {
				for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
					String name = entry.getKey();
					String value = entry.getValue();

					// if (StringUtils.isNotEmpty(name)) {
					if (!StringUtils.isEmpty(name)) {
						nameValuePairs.add(new BasicNameValuePair(name, value));
					}
				}
			}

			StringBuilder sb = new StringBuilder(url);
			sb.append(url.contains("?") ? "&" : "?");
			sb.append(EntityUtils.toString(new UrlEncodedFormEntity(nameValuePairs, "UTF-8")));
			HttpGet httpGet = new HttpGet(sb.toString());
			httpGet.setConfig(requestConfig);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			result = EntityUtils.toString(httpEntity, "utf-8");
			EntityUtils.consume(httpEntity);
		} catch (ClientProtocolException e) {
			//log.error(url + ":", e);
		} catch (IOException e) {
			//log.error(url + ":", e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				//log.error("http 请求关闭异常", e);
			}
		}
		return result;
	}

	/**
	 * GET请求 公共方法
	 *
	 * @param url
	 *            URL
	 * @param parameterMap
	 *            请求参数
	 * @return 返回结果
	 */
	public static String getWithHeaders(String url, Map<String, String> parameterMap, Map<String, String> headers) {
		String result = null;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			if (parameterMap != null) {
				for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
					String name = entry.getKey();
					String value = entry.getValue();

					// if (StringUtils.isNotEmpty(name)) {
					if (!StringUtils.isEmpty(name)) {
						nameValuePairs.add(new BasicNameValuePair(name, value));
					}
				}
			}

			StringBuilder sb = new StringBuilder(url);
			sb.append(url.contains("?") ? "&" : "?");
			sb.append(EntityUtils.toString(new UrlEncodedFormEntity(nameValuePairs, "UTF-8")));

			HttpGet httpGet = new HttpGet(sb.toString());
			httpGet.setConfig(requestConfig);

			for (Map.Entry<String, String> e : headers.entrySet()) {
				Header header = new BasicHeader(e.getKey(), e.getValue());

				httpGet.addHeader(header);
			}

			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			result = EntityUtils.toString(httpEntity, "utf-8");
			EntityUtils.consume(httpEntity);
		} catch (ClientProtocolException e) {
			//log.error(url + ":", e);
		} catch (IOException e) {
			//log.error(url + ":", e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				//log.error("http 请求关闭异常", e);
			}
		}
		return result;
	}

	/**
	 * GET请求
	 *
	 * @param url
	 * @param requestEntity
	 *            请求参数
	 * @return 返回结果
	 */
	public static String getSn(String url, String requestEntity) {
		String result = null;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			StringBuilder sb = new StringBuilder(url + "?data=" + requestEntity);
			HttpGet httpGet = new HttpGet(sb.toString());
			httpGet.setConfig(requestConfig);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			result = EntityUtils.toString(httpEntity, "utf-8");
			EntityUtils.consume(httpEntity);
		} catch (ClientProtocolException e) {
			//log.error(url + ":", e);
		} catch (IOException e) {
			//log.error(url + ":", e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				//log.error("http 请求关闭异常", e);
			}
		}
		return result;
	}
	private static String buildUrl(String host, String path, Map<String, String> querys) throws UnsupportedEncodingException {
		StringBuilder sbUrl = new StringBuilder();
		sbUrl.append(host);
		if (!StringUtils.isBlank(path)) {
			sbUrl.append(path);
		}
		if (null != querys) {
			StringBuilder sbQuery = new StringBuilder();
			for (Map.Entry<String, String> query : querys.entrySet()) {
				if (0 < sbQuery.length()) {
					sbQuery.append("&");
				}
				if (StringUtils.isBlank(query.getKey()) && !StringUtils.isBlank(query.getValue())) {
					sbQuery.append(query.getValue());
				}
				if (!StringUtils.isBlank(query.getKey())) {
					sbQuery.append(query.getKey());
					if (!StringUtils.isBlank(query.getValue())) {
						sbQuery.append("=");
						sbQuery.append(URLEncoder.encode(query.getValue(), "utf-8"));
					}
				}
			}
			if (0 < sbQuery.length()) {
				sbUrl.append("?").append(sbQuery);
			}
		}

		return sbUrl.toString();
	}
	/**
	 * Post String
	 *
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @param body
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doPost(String host, String path, String method,
									  Map<String, String> headers,
									  Map<String, String> querys,
									  String body)
			throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpPost request = new HttpPost(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		if (StringUtils.isNotBlank(body)) {
			request.setEntity(new StringEntity(body, "utf-8"));
		}

		return httpClient.execute(request);
	}

	private static HttpClient wrapClient(String host) {
		HttpClient httpClient = new DefaultHttpClient();
		if (host.startsWith("https://")) {
			sslClient(httpClient);
		}

		return httpClient;
	}

	private static void sslClient(HttpClient httpClient) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				public void checkClientTrusted(X509Certificate[] xcs, String str) {

				}
				public void checkServerTrusted(X509Certificate[] xcs, String str) {

				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			org.apache.http.conn.ssl.SSLSocketFactory ssf = new org.apache.http.conn.ssl.SSLSocketFactory(ctx);
			ssf.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = httpClient.getConnectionManager();
			SchemeRegistry registry = ccm.getSchemeRegistry();
			registry.register(new Scheme("https", 443, ssf));
		} catch (KeyManagementException ex) {
			throw new RuntimeException(ex);
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
	}

}
