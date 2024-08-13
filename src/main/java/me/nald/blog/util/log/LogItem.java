package me.nald.blog.util.log;


import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.util.Constants;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Data
public class LogItem {
    private static final int ETC_MESSAGE_NUMBER = 10;
    private static final SimpleDateFormat LOG_DATE_FORM = new SimpleDateFormat(Constants.LOG_DATETIME_FORMAT);

    private static final String LOG_NULL_MESSAGE = "";

    private String id;
    private Date startAt;
    private Date endAt;
    private String clientIP;
    private static String serverIP;
    private String appVersion;
    private String url;

    private String errMsg = "";
    private Object[] etcMessage;

    private boolean isWrite = false;

    private LogItem(HttpServletRequest request) {
        this.id = UUID.randomUUID().toString();
        this.startAt = new Date();
        this.etcMessage = new Object[ETC_MESSAGE_NUMBER];
        this.clientIP = getRemoteIP(request);
//        if (this.serverIP == null) {
//            this.serverIP = getServerIP();
//        }
        this.url = request.getRequestURL().toString();
    }

    public static LogItem of(HttpServletRequest request) {
        return new LogItem(request);
    }

    public static void genAndSet(HttpServletRequest request, String appVersion) {
        request.setAttribute(Constants.LOG_ITEM, of(request).appVersion(appVersion));
    }

    public static LogItem from(HttpServletRequest request) {
        return (LogItem) request.getAttribute(Constants.LOG_ITEM);
    }

    public LogItem id(String id) {
        setId(id);
        return this;
    }

    public LogItem startAt(Date startAt) {
        setStartAt(startAt);
        return this;
    }

    public LogItem endAt(Date endAt) {
        setEndAt(endAt);
        return this;
    }

    public LogItem clientIP(String clientIP) {
        setClientIP(clientIP);
        return this;
    }

    public LogItem url(String url) {
        setUrl(url);
        return this;
    }

    public LogItem errMsg(String errMsg) {
        setErrMsg(errMsg);
        return this;
    }

    public LogItem etcMessage(Object[] etcMessage) {
        setEtcMessage(etcMessage);
        return this;
    }

    public LogItem appVersion(String appVersion) {
        setAppVersion(appVersion);
        return this;
    }

    public void setMessage(int index, Object message) {
        if (index < etcMessage.length) {
            etcMessage[index] = message;
        }
    }

    public void setException(Exception e) {
        errMsg = e.getMessage();
        write(e);
    }

    private void write(Exception e) {
        log.error(id, e);
    }

    public synchronized void write() {
        if (!isWrite) {
            isWrite = true;
            log.trace("\n" + makeLine(Arrays.stream(this.etcMessage)
                    .map(a -> Objects.isNull(a) ? LOG_NULL_MESSAGE : a.toString())
                    .collect(Collectors.toList())));
        }
    }

    private String makeLine(List<String> items) {
        this.endAt = new Date();

        List<String> inLine = new ArrayList<>();

        if (!StringUtils.isEmpty(appVersion)) {
            inLine.add(appVersion);
        }
        inLine.add(this.id);
        inLine.add(LOG_DATE_FORM.format(this.startAt));
        inLine.add(LOG_DATE_FORM.format(this.endAt));
        inLine.add(String.valueOf(this.endAt.getTime() - this.startAt.getTime()));
        inLine.add(serverIP);
        inLine.add(clientIP);
        inLine.add(url);
        inLine.add(errMsg);
        inLine.addAll(items);

        return String.join(Constants.LOG_DELIMITER, inLine);
    }

    private String getRemoteIP(HttpServletRequest request){
        String ip = request.getHeader("X-FORWARDED-FOR");

        //proxy 환경일 경우
        if (ip == null || ip.length() == 0) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        //웹로직 서버일 경우
        if (ip == null || ip.length() == 0) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0) {
            ip = request.getRemoteAddr() ;
        }

        return ip;
    }

    private String getServerIP() {
        String ip = null;
        try {
            InetAddress ia = InetAddress.getLocalHost();
            ip = ia.getHostAddress();
        } catch (UnknownHostException e) {}
        return ip;
    }
}
