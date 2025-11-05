package org.jrebel.server;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jrebel.util.JrebelSign;
import org.jrebel.util.rsasign;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainServer extends AbstractHandler {
    
    private static final String SERVER_GUID = "a1b4aea8-b031-4302-b602-670a990272cb";

    private static Map<String, String> parseArguments(String[] args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Error in argument's length ");
        }

        Map<String, String> params = new HashMap<>();

        for (int i = 0, len = args.length; i < len; ) {
            String argName = args[i++];

            if (argName.charAt(0) == '-') {
                if (argName.length() < 2) {
                    throw new IllegalArgumentException("Error at argument " + argName);
                }
                argName = argName.substring(1);
            }

            params.put(argName, args[i++]);
        }

        return params;
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> arguments = parseArguments(args);
        String port = arguments.getOrDefault("p", "9020");

        if (!port.matches("\\d+")) {
            port = "9020";
        }

        Server server = new Server(Integer.parseInt(port));
        server.setHandler(new MainServer());
        server.start();

        System.out.println("License Server started at http://localhost:" + port);
        System.out.println("JRebel 7.1 and earlier version Activation address was: http://localhost:" + port + "/{tokenname}, with any email.");
        System.out.println("JRebel 2018.1 and later version Activation address was: http://localhost:" + port + "/{guid}(eg:http://localhost:" + port + "/" + getUUID() + "), with any email.");

        server.join();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        // 设置默认状态码
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        //System.out.println("target:"+target);
        if ("/".equals(target)) {
            indexHandler(baseRequest, request, response);
        } else if ("/jrebel/leases".equals(target) || "/agent/leases".equals(target)) {
            jrebelLeasesHandler(baseRequest, request, response);
        } else if ("/jrebel/leases/1".equals(target) || "/agent/leases/1".equals(target)) {
            jrebelLeases1Handler(baseRequest, request, response);
        } else if ("/jrebel/validate-connection".equals(target)) {
            jrebelValidateHandler(baseRequest, request, response);
        } else if ("/rpc/ping.action".equals(target)) {
            pingHandler(baseRequest, request, response);
        } else if ("/rpc/obtainTicket.action".equals(target)) {
            obtainTicketHandler(baseRequest, request, response);
        } else if ("/rpc/releaseTicket.action".equals(target)) {
            releaseTicketHandler(baseRequest, request, response);
        }
    }

    private void sendJsonResponse(HttpServletResponse response, JSONObject json) throws IOException {
        response.setContentType("application/json; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(json.toJSONString());
    }

    private void sendXmlResponse(HttpServletResponse response, String xmlContent) throws IOException {
        String signature = rsasign.Sign(xmlContent);
        String body = "<!-- " + signature + " -->\n" + xmlContent;
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(body);
    }

    private void jrebelValidateHandler(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        JSONObject json = new JSONObject();
        json.put("serverVersion", "3.2.4");
        json.put("serverProtocolVersion", "1.1");
        json.put("serverGuid", SERVER_GUID);
        json.put("groupType", "managed");
        json.put("statusCode", "SUCCESS");
        json.put("company", "Administrator");
        json.put("canGetLease", true);
        json.put("licenseType", 1);
        json.put("evaluationLicense", false);
        json.put("seatPoolType", "standalone");
        sendJsonResponse(response, json);
    }

    private void jrebelLeases1Handler(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        JSONObject json = new JSONObject();
        json.put("serverVersion", "3.2.4");
        json.put("serverProtocolVersion", "1.1");
        json.put("serverGuid", SERVER_GUID);
        json.put("groupType", "managed");
        json.put("statusCode", "SUCCESS");
        json.put("msg", null);
        json.put("statusMessage", null);

        String username = request.getParameter("username");
        if (StringUtils.isNotBlank(username)) {
            json.put("company", username);
        }

        sendJsonResponse(response, json);
    }

    private void jrebelLeasesHandler(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        String clientRandomness = request.getParameter("randomness");
        String username = request.getParameter("username");
        String guid = request.getParameter("guid");
        String reqOffline = request.getParameter("offline");
        boolean offline = Boolean.parseBoolean(reqOffline);
        if(StringUtils.isNotEmpty(request.getParameter("oldGuid"))){
            offline = true;
        }
        String validFrom = "";
        String validUntil = "";
        
        String clientTime = request.getParameter("clientTime");

        try {
            long clientTimeMillis = Long.parseLong(clientTime);
            validFrom = clientTime;
            validUntil = String.valueOf(clientTimeMillis + 180L * 24 * 60 * 60 * 1000);
        } catch (NumberFormatException ignored) {
            // 忽略非法输入，保持默认空值
        }

        JSONObject json = new JSONObject();
        json.put("serverVersion", "3.2.4");
        json.put("serverProtocolVersion", "1.1");
        json.put("serverGuid", SERVER_GUID);
        json.put("groupType", "managed");
        json.put("id", 1);
        json.put("licenseType", 1);
        json.put("evaluationLicense", false);
        json.put("signature", "OJE9wGg2xncSb+VgnYT+9HGCFaLOk28tneMFhCbpVMKoC/Iq4LuaDKPirBjG4o394/UjCDGgTBpIrzcXNPdVxVr8PnQzpy7ZSToGO8wv/KIWZT9/ba7bDbA8/RZ4B37YkCeXhjaixpmoyz/CIZMnei4q7oWR7DYUOlOcEWDQhiY=");
        json.put("serverRandomness", "H2ulzLlh7E0=");
        json.put("seatPoolType", "standalone");
        json.put("statusCode", "SUCCESS");
        json.put("offline", offline);
        if (offline) {
            json.put("validFrom", validFrom);
            json.put("validUntil", validUntil);
        }
        json.put("company", "Administrator");
        json.put("orderId", "");
        json.put("zeroIds", new JSONArray());
        json.put("licenseValidFrom", validFrom);
        json.put("licenseValidUntil", validUntil);

        if (clientRandomness == null || username == null || guid == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            JrebelSign jrebelSign = new JrebelSign();
            jrebelSign.toLeaseCreateJson(clientRandomness, guid, offline, validFrom, validUntil);
            json.put("signature", jrebelSign.getSignature());
            json.put("company", username);
            sendJsonResponse(response, json);
        }
    }

    private void releaseTicketHandler(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        String salt = request.getParameter("salt");
        if (salt == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String xmlContent = "<ReleaseTicketResponse><message></message><responseCode>OK</responseCode><salt>" + escapeXml(salt) + "</salt></ReleaseTicketResponse>";
        sendXmlResponse(response, xmlContent);
    }

    private void obtainTicketHandler(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        String salt = request.getParameter("salt");
        String username = request.getParameter("userName");
        if (salt == null || username == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String prolongationPeriod = "607875500";
        String xmlContent = "<ObtainTicketResponse><message></message><prolongationPeriod>" + prolongationPeriod + "</prolongationPeriod><responseCode>OK</responseCode><salt>" + escapeXml(salt) + "</salt><ticketId>1</ticketId><ticketProperties>licensee=" + escapeXml(username) + "\tlicenseType=0\t</ticketProperties></ObtainTicketResponse>";
        sendXmlResponse(response, xmlContent);
    }

    private void pingHandler(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        String salt = request.getParameter("salt");
        if (salt == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String xmlContent = "<PingResponse><message></message><responseCode>OK</responseCode><salt>" + escapeXml(salt) + "</salt></PingResponse>";
        sendXmlResponse(response, xmlContent);
    }

    private void indexHandler(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        String protocol = request.getHeader("X-Forwarded-Proto");
        String licenseUrl = protocol + "://" + request.getServerName();;
        if (StringUtils.equals("https", protocol)) {
        } else {
            licenseUrl = licenseUrl + ":" + request.getServerPort();
        }

        StringBuilder html = new StringBuilder("<h3>使用说明（Instructions for use）</h3>");
        html.append("<hr/>");
        html.append("<h1>Hello,This is a Jrebel License Server!</h1>");
        html.append("<p>JRebel 7.1 and earlier version Activation address was: <span style='color:red'>")
                .append(licenseUrl).append("/{tokenname}")
                .append("</span>, with any email.");
        html.append("<p>JRebel 2018.1 and later version Activation address was: ")
                .append(licenseUrl).append("/{guid}")
                .append("(eg:<span style='color:red'>")
                .append(licenseUrl).append("/").append(getUUID())
                .append("</span>), with any email.");

        html.append("<hr/>");

        html.append("<h1>Hello，此地址是 Jrebel License Server!</h1>");
        html.append("<p>JRebel 7.1 及旧版本激活地址: <span style='color:red'>")
                .append(licenseUrl).append("/{tokenname}")
                .append("</span>, 以及任意邮箱地址。");
        html.append("<p>JRebel 2018.1+ 版本激活地址: ")
                .append(licenseUrl).append("/{guid}")
                .append("(例如：<span style='color:red'>")
                .append(licenseUrl).append("/").append(getUUID())
                .append("</span>), 以及任意邮箱地址。");

        response.getWriter().println(html);
    }

    // XML 转义工具函数
    private String escapeXml(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > '>') {
                out.append(c);
            } else switch (c) {
                case '&':  out.append("&amp;"); break;
                case '<':  out.append("&lt;");  break;
                case '>':  out.append("&gt;");  break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&apos;"); break;
                default: out.append(c); break;
            }
        }
        return out.toString();
    }
    
    private static String getUUID(){
        return UUID.randomUUID().toString();
    }
}
