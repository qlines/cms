package smsgateway.webadmin.servlet;

import hippoping.smsgw.api.comparator.txsmsdownload.TxSmsDownloadSortByCarrier;
import hippoping.smsgw.api.comparator.txsmsdownload.TxSmsDownloadSortByKeyword;
import hippoping.smsgw.api.comparator.txsmsdownload.TxSmsDownloadSortByMsisdn;
import hippoping.smsgw.api.comparator.txsmsdownload.TxSmsDownloadSortByReceipt;
import hippoping.smsgw.api.comparator.txsmsdownload.TxSmsDownloadSortByServiceName;
import hippoping.smsgw.api.db.DeliveryReport;
import hippoping.smsgw.api.db.LogEvent;
import hippoping.smsgw.api.db.MessageSms;
import hippoping.smsgw.api.db.MessageWap;
import hippoping.smsgw.api.db.OperConfig;
import hippoping.smsgw.api.db.ServiceElement;
import hippoping.smsgw.api.db.TxQueue;
import hippoping.smsgw.api.db.TxSmsDownload;
import hippoping.smsgw.api.db.TxSmsDownloadFactory;
import hippoping.smsgw.api.db.User;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lib.common.DatetimeUtil;
import lib.common.StringConvert;

public class ReportSmsDownloadServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(ReportSmsDownloadServlet.class.getName());
    protected int rows = 0;
    protected String old_orderby = "";
    protected int sort = 0;

    private void sort(List<TxSmsDownload> txSmsDownloadList, String field, int swap) {
        Comparator comparator = null;

        if ((this.old_orderby != null) && (this.old_orderby.equals(field))) {
            if (swap == 1) {
                this.sort = (++this.sort % 2);
            }
        } else {
            this.sort = 0;
            this.old_orderby = field;
        }

        if (field.equals("msisdn")) {
            comparator = new TxSmsDownloadSortByMsisdn();
        } else if (field.equals("carrier")) {
            comparator = new TxSmsDownloadSortByCarrier();
        } else if (field.equals("service")) {
            comparator = new TxSmsDownloadSortByServiceName();
        } else if (field.equals("keyword")) {
            comparator = new TxSmsDownloadSortByKeyword();
        } else if (field.equals("receipt")) {
            comparator = new TxSmsDownloadSortByReceipt();
        }
        Collections.sort(txSmsDownloadList, comparator);
        if (this.sort == 1) {
            Collections.reverse(txSmsDownloadList);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            User user = (User) request.getSession().getAttribute("USER");
            if (user == null) {
                out.print("<script>window.location='logout?msg=Your session has been expired! please relogin the page.'</script>");
            } else {
                List txSmsDownloadList = (List) request.getSession().getAttribute("txSmsDownloadList");
                List keywordList = (List) request.getSession().getAttribute("keywordList");

                String srvcid = "";
                String operid = "";
                String fdate = "";
                String tdate = "";
                String msisdn = request.getParameter("msisdn");
                String orderby = request.getParameter("orderby");
                String page = request.getParameter("page");
                String swap = request.getParameter("swap");
                String keyword = request.getParameter("keyword");

                String cmd = request.getParameter("cmd");

                if ((page == null) || (page.equals(""))) {
                    page = "1";
                }

                if ((cmd != null) && (cmd.equals("refresh"))) {
                    srvcid = request.getParameter("srvcid");
                    operid = request.getParameter("operid");
                    fdate = request.getParameter("fdate");
                    tdate = request.getParameter("tdate");
                    this.rows = Integer.parseInt(request.getParameter("rows"));

                    if ((srvcid == null) || (srvcid.equals(""))) {
                        srvcid = "-1";
                    }

                    OperConfig.CARRIER oper = null;
                    if ((operid == null) || (operid.equals(""))) {
                        operid = "-1";
                    } else {
                        oper = OperConfig.CARRIER.fromId(Integer.parseInt(operid));
                    }

                    String dt_fmt = "dd-MM-yyyy";

                    if ((!fdate.matches("^(0[1-9]|[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012]|[1-9])-(19|20)\\d{2}$")) 
                            || (!tdate.matches("^(0[1-9]|[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012]|[1-9])-(19|20)\\d{2}$"))) {
                        out.println("Date format error");
                        throw new ServletException("Date format error!!");
                    }
                    try {
                        Date from = new SimpleDateFormat(dt_fmt).parse(fdate);
                        Date to = new SimpleDateFormat(dt_fmt + " HH:mm:ss").parse(tdate + " 23:59:59");

                        request.getSession().setAttribute("txSmsDownloadList", 
                                new TxSmsDownloadFactory(from, to, oper, Integer.parseInt(srvcid), msisdn, user, keyword).getTxSmsDownloadList());

                        txSmsDownloadList = (List) request.getSession().getAttribute("txSmsDownloadList");

                        request.getSession().setAttribute("keywordList", TxSmsDownloadFactory.getKeywordList(txSmsDownloadList));
                        keywordList = (List) request.getSession().getAttribute("keywordList");

                        LogEvent.log(LogEvent.EVENT_TYPE.REPORT_SMSDOWNLOAD, LogEvent.EVENT_ACTION.SEARCH, "", (User) request.getSession().getAttribute("USER"), msisdn, oper, Integer.parseInt(srvcid), 0, 0, LogEvent.LOG_LEVEL.INFO);
                    } catch (ParseException e) {
                        log.log(Level.SEVERE, "parse failed!!", e);
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "date format error!!", e);
                    }
                } else if ((cmd != null) && (cmd.equals("filter")) && (keyword != null)) {
                    if (keyword.equals("-1")) {
                        txSmsDownloadList = (List) request.getSession().getAttribute("txSmsDownloadList");
                    } else {
                        txSmsDownloadList = new ArrayList();
                        List _txSmsDownloadList = (List) request.getSession().getAttribute("txSmsDownloadList");

                        keywordList = (List) request.getSession().getAttribute("keywordList");

                        for (int j = 0; j < _txSmsDownloadList.size(); j++) {
                            TxSmsDownload smsdownload = (TxSmsDownload) _txSmsDownloadList.get(j);
                            if (((String) keywordList.get(Integer.parseInt(keyword))).equalsIgnoreCase(smsdownload.getKeyword())) {
                                txSmsDownloadList.add(smsdownload);
                            }
                        }
                    }

                }

                String keywordDDL = "";
                keywordDDL = keywordDDL + "<select name='keyword' onchange='javascript:doFilter()'>";
                keywordDDL = keywordDDL + "<option value='-1'>All Keywords</option>";
                for (int j = 0; j < keywordList.size(); j++) {
                    //if (keyword != null) {
                        keywordDDL = keywordDDL + "<option value='" + j + "'" 
                                + ((keyword != null) 
                                    && (!keyword.equals("-1")) 
                                    && (StringConvert.isDigit(keyword)) 
                                    && (((String) keywordList.get(Integer.parseInt(keyword))).equalsIgnoreCase(((String) keywordList.get(j)).toString())) 
                                ? " selected" 
                                : "") 
                                + ">" 
                                + ((String) keywordList.get(j)).toString() + "</option>";
                    //}
                }

                keywordDDL = keywordDDL + "</select>";

                if ((txSmsDownloadList != null) && (txSmsDownloadList.size() > 0)) {
                    sort(txSmsDownloadList, orderby, (swap != null) && (swap.equals("1")) ? 1 : 0);
                }

                int pg = txSmsDownloadList.size() / this.rows + (txSmsDownloadList.size() % this.rows != 0 ? 1 : 0);

                pg = pg == 0 ? 1 : pg;
                out.println("<html><head>"
                        + "    <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>"
                        + "    <link href='./css/cv.css' rel='stylesheet' type='text/css'>"
                        + "    <link href='./css/niftyCorners.css' rel='stylesheet' type='text/css'>"
                        + "    <link href='./css/niftyPrint.css' rel='stylesheet' type='text/css' media='print'>"
                        + "    <style type='text/css'>"
                        + "        body{margin:0px; padding: 0px; background: white;"
                        + "            font: 100.01% 'Trebuchet MS',Verdana,Arial,sans-serif}"
                        + "        h1,h2,p{margin: 0 10px}"
                        + "        h1{font-size: 250%;color: #FFF}"
                        + "        h2{font-size: 200%;color: #f0f0f0}"
                        + "        p{padding-bottom:1em}"
                        + "        h2{padding-top: 0.3em}"
                        + "        div#memberViewContent {background: #377CB1;}"
                        + "    </style>"
                        + "    <script src='./js/nifty.js' type='text/javascript'></script>"
                        + "    <script src='./js/utils.js' type='text/javascript'></script>"
                        + "    <script src='./js/filter_input.js' type='text/javascript'></script>"
                        + "    <script>"
                        + "    function validate_page(page, maxpage) {"
                        + "        var frm = document.forms[\"reloadFrm\"];"
                        + "       if (page=='') {alert('Please enter page number.'); frm.page.value=" + page + ";return false;}" 
                        + "       else if (page>maxpage || page<=0) {alert('Page ' + page + ' not found!'); frm.page.value=" + page + ";return false;}" 
                        + "       else {frm.submit();}" 
                        + "    }" 
                        + "    function goto_page(page) {" 
                        + "       frm=document.forms[\"reloadFrm\"];" 
                        + "       frm.page.value=page;" 
                        + "       frm.submit();" 
                        + "    }" 
                        + "    function doFilter() {" 
                        + "       frm=document.forms[\"reloadFrm\"];" 
                        + "       frm.cmd.value='filter';" 
                        + "       frm.submit();" 
                        + "    }" 
                        + "    function doHistory(operid, srvcid, msisdn)" 
                        + "    {" 
                        + "         window.open(\"message_history.jsp\" +" 
                        + "    \"?operid=\" + operid +" 
                        + "    \"&srvcid=\" + srvcid +" 
                        + "    \"&msisdn=\" + msisdn +" 
                        + "    \"&cmd=refresh\"" 
                        + "    ,null, \"height=650,width=732,status=yes,toolbar=no,menubar=no,location=no,scrollbars=yes,resizable=no\");" 
                        + "    return;" 
                        + "    }" 
                        + "    </script>" 
                        + "</head>" 
                        + "<body style='background-color:#FFF;'>" 
                        + "   <div id='data' style='padding: 0 10px 0 10px;width:97%;^width:100%;_width:100%;;'>" 
                        + "       <form name='reloadFrm' method='POST' onsubmit='return validate_page(document.forms[\"reloadFrm\"].page.value, " + pg + ");'>" 
                        + "       <input type=hidden name=cmd value=''>" 
                        + "       <input type=hidden name=orderby value='" 
                        + orderby + "'>" 
                        + "       <input type=hidden name=swap value='0'>" 
                        + "       <input type=hidden name=csv value='0'>" 
                        + "       <div class='floatl' style='font-size:75%; padding-left:5px;'><b>Total " + txSmsDownloadList.size() + " record(s) found." 
                        + "           (Page " + page + " of " + pg + ")</b> " 
                        + "           | Export <a href='javascript:window.location=\"./reportSmsDownloadServlet?csv=1&page=" + page + "\";'><img src='./images/csv_2.gif'></a>" 
                        + "       </div>" 
                        + "       <div class='floatr'>" 
                        + "         <span style='padding:0;'>" 
                        + (Integer.parseInt(page) > 1 
                                ? "<a href='javascript:goto_page(" + (Integer.parseInt(page) - 1) + ")'><img src='images/previous.gif' border=0 style='vertical-align:middle;'></a>" 
                                : "<img src='images/previous_dis.gif' border=0 style='vertical-align:middle;'>") 
                        + (Integer.parseInt(page) < pg 
                                ? "<a href='javascript:goto_page(" + (Integer.parseInt(page) + 1) + ")'><img src='images/next.gif' border=0 style='vertical-align:middle;'></a>" 
                                : "<img src='images/next_dis.gif' border=0 style='vertical-align:middle;'>") 
                        + "         </span>" 
                        + "           <span style='font-size:75%; padding-left:5px; vertical-align:middle;'>Goto page</span> <input type=text name=page size=2 value='" + page + "' onkeypress='return filter_digit_char(event)'>" 
                        + "           <input type=submit value=go>" 
                        + "       </div>" 
                        + "       <table class='table3' style='width:100%;padding:0;'>" 
                        + "       <tr>" 
                        + "           <th width='3%'>No.</th>" 
                        + "           <th width='20%'>" 
                        + (orderby.equals("receipt") 
                                ? "<img src='images/puce_" + (this.sort == 0 ? "top" : "bottom") + ".gif' border=0>" 
                                : "") 
                        + "<a href='javascript:frm=document.forms[\"reloadFrm\"];" 
                        + "frm.orderby.value=\"receipt\";frm.swap.value=1;frm.submit();'>Receipt</a></th>" 
                        + "           <th width='20%'>" + (orderby.equals("service") ? "<img src='images/puce_" + (this.sort == 0 ? "top" : "bottom") + ".gif' border=0>" : "") 
                        + "<a href='javascript:frm=document.forms[\"reloadFrm\"];" + "frm.orderby.value=\"service\";frm.swap.value=1;frm.submit();'>Service</a></th>" 
                        + "           <th width='10%'>" + (orderby.equals("msisdn") ? "<img src='images/puce_" + (this.sort == 0 ? "top" : "bottom") + ".gif' border=0>" : "") 
                        + "<a href='javascript:frm=document.forms[\"reloadFrm\"];" + "frm.orderby.value=\"msisdn\";frm.swap.value=1;frm.submit();'>MSISDN</a></th>" 
                        + "           <th width='7%'>" + (orderby.equals("carrier") ? "<img src='images/puce_" + (this.sort == 0 ? "top" : "bottom") + ".gif' border=0>" : "") 
                        + "<a href='javascript:frm=document.forms[\"reloadFrm\"];" + "frm.orderby.value=\"carrier\";frm.swap.value=1;frm.submit();'>Carrier</a></th>" 
                        + "           <th width='35%'>" + (orderby.equals("keyword") ? "<img src='images/puce_" + (this.sort == 0 ? "top" : "bottom") + ".gif' border=0>" : "") 
                        + "<a href='javascript:frm=document.forms[\"reloadFrm\"];" + "frm.orderby.value=\"keyword\";frm.swap.value=1;frm.submit();'>Keyword</a> " + keywordDDL + "</th>" 
                        + "           <th width='5%'>Status</th>" 
                        + "       </tr>" 
                        + "       </form>");

                int sindex = (Integer.parseInt(page) - 1) * this.rows;
                int eindex = sindex + this.rows;
                for (int i = sindex; (i < txSmsDownloadList.size()) && (i < eindex); i++) {
                    TxQueue txq = null;
                    try {
                        txq = TxQueue.getTxQueueByRxid(((TxSmsDownload) txSmsDownloadList.get(i)).getMsisdn(), ((TxSmsDownload) txSmsDownloadList.get(i)).getSrvc_main_id(), ((TxSmsDownload) txSmsDownloadList.get(i)).getOper().getId(), ((TxSmsDownload) txSmsDownloadList.get(i)).getRx_id());
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "txQueue not found by rxid[{0}]", Long.valueOf(((TxSmsDownload) txSmsDownloadList.get(i)).getRx_id()));
                    }

                    ServiceElement se = null;
                    try {
                        se = new ServiceElement(((TxSmsDownload) txSmsDownloadList.get(i)).getSrvc_main_id(), 
                                ((TxSmsDownload) txSmsDownloadList.get(i)).getOper().getId(), 
                                ServiceElement.SERVICE_TYPE.SMSDOWNLOAD.getId(), 
                                ServiceElement.SERVICE_STATUS.ON.getId() | ServiceElement.SERVICE_STATUS.TEST.getId());
                    } catch (Exception e) {
                    }
                    
                    boolean isCharged = se.chrg_flg.equals("MO") || txq.chrg_flg.equals("MT");

                    int chrg_success = 3;
                    String[] chrg_img = {"warning16.gif", "delete16.gif", "accept16.gif", "blank.gif"};
                    if (txq == null) {
                        chrg_success = 0;
                    } else if (((TxSmsDownload) txSmsDownloadList.get(i)).getOper().getId() == OperConfig.CARRIER.AIS_LEGACY.getId()) {
                        chrg_success = (txq.status_code == 0) && (txq.status_desc != null) && (txq.status_desc.substring(0, 2).equals("OK")) ? 2 : 1;
                    } else if (se.chrg_flg.equals("MO")) {
                        chrg_success = 2;
                    } else if (txq.chrg_flg.equals("MT")) {
                        try {
                            List drList = DeliveryReport.get(txq, ((TxSmsDownload) txSmsDownloadList.get(i)).getMsisdn());
                            if (drList == null) {
                                chrg_success = 0;
                            } else {
                                for (int j = 0; j < drList.size(); j++) {
                                    chrg_success = ((DeliveryReport) drList.get(j)).isChargeSuccess() ? 2 : 1;
                                    if (chrg_success == 2) {
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                    String style = i % 2 == 0 ? "" : " class='d0'";
                    out.print("<tr" + style + "><td>" + (i + 1) + "</td>");
                    out.print("<td>" + new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(DatetimeUtil.toDate(((TxSmsDownload) txSmsDownloadList.get(i)).getRecv_dt())) + "</td>");
                    out.print("<td>" + ((TxSmsDownload) txSmsDownloadList.get(i)).getSrvc_name() + "</td>");
                    out.print("<td>" + (user.getType().getId() <= User.USER_TYPE.ADMIN.getId() ? "<a href='javascript:doHistory(" + ((TxSmsDownload) txSmsDownloadList.get(i)).getOper().getId() + "," + ((TxSmsDownload) txSmsDownloadList.get(i)).getSrvc_main_id() + ",\"" + ((TxSmsDownload) txSmsDownloadList.get(i)).getMsisdn() + "\");'>" : "") + (user.getType().getId() <= User.USER_TYPE.SUPERVISOR.getId() ? ((TxSmsDownload) txSmsDownloadList.get(i)).getMsisdn() : new StringBuilder().append(((TxSmsDownload) txSmsDownloadList.get(i)).getMsisdn().substring(0, 9)).append("XX").toString()) + (user.getType().getId() <= User.USER_TYPE.ADMIN.getId() ? "</a>" : "") + "</td>");

                    out.print("<td>" + ((TxSmsDownload) txSmsDownloadList.get(i)).getOper().toString() + "</td>");
                    String message = null;
                    if (txq != null) {
                        try {
                            switch (txq.content_type) {
                                case SMS:
                                    MessageSms sms = new MessageSms(txq.content_id);
                                    message = new String();
                                    for (int isub = 0; isub < sms.getMessage_num(); isub++) {
                                        message = message + sms.getContent()[isub];
                                    }
                                    break;
                                case WAP:
                                    MessageWap wap = new MessageWap(txq.content_id);
                                    message = wap.title + "[" + wap.url + "]";
                            }
                        } catch (Exception e) {
                        }
                    }

                    out.print("<td" + (message != null ? " title='" + message + "'" : "") + ">" 
                            + ((TxSmsDownload) txSmsDownloadList.get(i)).getKeyword() 
                            + (isCharged 
                                    ? "<img src='./images/favorite16.gif'>" 
                                    : "") 
                            + "</td>");

                    out.print("<td><img src='./images/" + chrg_img[chrg_success] + "'" + (chrg_success == 0 ? " title='no dr found'" : "") + ">" + "</td>");

                    out.print("</tr>");
                }
                out.println("<tr><td colspan='6' style='text-align:right;padding:30px 30px 10px 0;line-height:1.5em'><img src='./images/favorite16.gif' style='vertical-align:middle;'> Charged message&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src='./images/warning16.gif' style='vertical-align:middle;'> Unknown result<BR><img src='./images/accept16.gif' style='vertical-align:middle;'> Received&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src='./images/delete16.gif' style='vertical-align:middle;'> Charged fail</td></tr>");

                out.println("</table></div></body></html>");
            }
        } finally {
            out.close();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        OutputStreamWriter out = new OutputStreamWriter(response.getOutputStream(), "ISO8859_1");
        String csv = request.getParameter("csv");

        MultipartResponse multi = new MultipartResponse(response);
        try {
            User user = (User) request.getSession().getAttribute("USER");
            if (user == null) {
                return;
            }

            if ((csv != null) && (csv.equals("1"))) {
                List txSmsDownloadList = (List) request.getSession().getAttribute("txSmsDownloadList");

                if (txSmsDownloadList == null) {
                    response.sendError(204);
                }

                multi.startResponse("text/csv;charset=tis-620");
                response.setHeader("Content-disposition", "attachment; filename=Sms_Download_Report.csv");

                out.append("No.,Receipt,Service,MSISDN,Carrier,Keyword,\r\n");
                for (int i = 0; i < txSmsDownloadList.size(); i++) {
                    out.append(i + 1 + ",");
                    out.append(new SimpleDateFormat("MM/dd/yy HH:mm:ss").format(DatetimeUtil.toDate(((TxSmsDownload) txSmsDownloadList.get(i)).getRecv_dt())) + ",");
                    out.append("\"" + StringConvert.Unicode2ASCII2(((TxSmsDownload) txSmsDownloadList.get(i)).getSrvc_name()) + "\",");
                    out.append((user.getType().getId() <= User.USER_TYPE.SENIOR.getId() ? ((TxSmsDownload) txSmsDownloadList.get(i)).getMsisdn() : new StringBuilder().append(((TxSmsDownload) txSmsDownloadList.get(i)).getMsisdn().substring(0, 9)).append("XX").toString()) + ",");
                    out.append(((TxSmsDownload) txSmsDownloadList.get(i)).getOper().toString() + ",");
                    out.append("\"" + StringConvert.Unicode2ASCII2(((TxSmsDownload) txSmsDownloadList.get(i)).getKeyword()) + "\",");
                    out.append("\r\n");
                }
                out.flush();
                multi.endResponse();
            }
        } finally {
            multi.finish();
            out.close();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    public String getServletInfo() {
        return "Short description";
    }
}