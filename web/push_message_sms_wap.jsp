<%--
    Document   : push_message_text
    Created on : 12 มี.ค. 2553, 12:47:23
    Author     : Administrator
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="hippoping.smsgw.api.db.ServiceElement.SERVICE_TYPE" %>
<%@page import="hippoping.smsgw.api.db.TxQueue" %>
<%@page import="hippoping.smsgw.api.db.MessageWap" %>
<%@page import="lib.common.DatetimeUtil" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link href="./css/cv.css" rel="stylesheet" type="text/css">
        <link href="./css/niftyCorners.css" rel="stylesheet" type="text/css">
        <link href="./css/niftyPrint.css" rel="stylesheet" type="text/css" media="print">
        <link rel="stylesheet" type="text/css" href="./css/dashboard.css" media="screen" />
        <link href="./css/infobox01.css" rel="stylesheet" type="text/css">
        <style type="text/css">
            body{margin:0px; padding: 0px; background: white;
                 font: 100.01% "Trebuchet MS",Verdana,Arial,sans-serif}
            h1,h2,p{margin: 0 10px}
            h1{font-size: 250%;color: #FFF}
            h2{font-size: 200%;color: #f0f0f0}
            p{padding-bottom:1em}
            h2{padding-top: 0.3em}
        </style>
        <script src="./js/nifty.js" type="text/javascript"></script>
        <script src='./js/utils.js' type='text/javascript'></script>
        <script src='./js/filter_input.js' type='text/javascript'></script>
        <script src='./js/webstyle.js' type='text/javascript'></script>
        <script src='./js/datetime.js' type='text/javascript'></script>
        <%
            String _srvcid = "";
            String _operid = "";
            String _title = "";
            String _url = "";
            String _schedule = "";
            String _now = "checked";
            String _date = "";
            String _time = "";

            String _txqid = request.getParameter("txqid")!=null?request.getParameter("txqid"):"";
            if (!_txqid.isEmpty()) {
                int txqid = Integer.parseInt(_txqid);
                TxQueue txQueue = new TxQueue(txqid);
                MessageWap messageWap = new MessageWap(txQueue.content_id);

                _srvcid = String.valueOf(txQueue.srvc_main_id);
                _operid = String.valueOf(txQueue.oper_id);
                _title = messageWap.title;
                _url = messageWap.url;
                _schedule = "checked";
                _now = "";
                _date = DatetimeUtil.changeDateFormat(txQueue.deliver_dt, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd");
                _time = DatetimeUtil.changeDateFormat(txQueue.deliver_dt, "yyyy-MM-dd HH:mm:ss", "HH:mm");
            }

            int srvc_type = SERVICE_TYPE.WAP.getId() | SERVICE_TYPE.SUBSCRIPTION.getId();
        %>
        <jsp:include page="./services_bean.jsp">
            <jsp:param name="srvc_type" value="<%=srvc_type%>" />
        </jsp:include>
        <script type="text/javascript">
            var now = new Date();

            var hh = now.getHours();
            var mn = now.getMinutes();

            function updateServiceOptions(oper_id, obj) {
                var frm = document.pushMessageWapFrm;
                removeAllOptions(obj);

                if (!optionsArry[oper_id]) {
                    var tmpArry = new Array(); // create Array 2 dimensions
                    tmpArry[0] = new Array(2);
                    tmpArry[0][0] = "0";
                    tmpArry[0][1] = "No service available";

                    addOption_list(obj, tmpArry);
                    frm.srvc_main_id.disabled = true;
                }
                else {
                    if ("<%=_srvcid%>" == "")
                        addOption_list(obj, optionsArry[oper_id]);
                    else
                        addOption_list(obj, optionsArry[oper_id], "<%=_srvcid%>");
                    frm.srvc_main_id.disabled = false;
                }
            }

            function updateSendtimeBox() {
                getElement('senddate').disabled = (getElement('schedule1').checked)?true:false;
                getElement('sendtime').disabled = (getElement('schedule1').checked)?true:false;
            }

            function updateOperSelect(id) {
                obj = getElement('operSelectDiv');
                var str = "";
                for (var i=1;i<operIdArry.length;i++) {
                    str += "<input type='checkbox' name='oper' id='oper" + operIdArry[i] + "'"
                        + ((isServiceInOper(id, operIdArry[i]))
                        ?(("<%=_operid%>" == "" || ("<%=_operid%>" != "" && "<%=_operid%>" == i))?" checked":"")
                        :" disabled")
                        + ">" + operNameArry[i] + " ";
                }
                obj.innerHTML = str;
            }

            function whichOper() {
                var opers = new Array(4,3,2,5,1,6);
                for (i in opers) {
                    var obj = getElement('oper' + opers[i]);
                    if (obj && !obj.disabled && obj.checked) {
                        return opers[i];
                    }
                }
            }

            function getServiceDetails(srvc_main_id, oper_id) {
                var url = "service_details.jsp?srvc_main_id=" + srvc_main_id + "&oper_id=" + oper_id;
                var obj = getElement('service_info_box');
                if (obj)
                    obj.innerHTML = ajaxPost(url);
            }
            
            function _onsubmit() {
                var frm = document.pushMessageWapFrm;
                if (frm.url.value.trim().length == 0) {
                    alert('Please enter the URL!');
                    frm.url.focus();
                    return false;
                }
                if (frm.title.value.trim().length == 0) {
                    alert('Please enter the title!');
                    frm.title.focus();
                    return false;
                }

                // check wap url format
                if (!isUrl(frm.url.value.trim())) {
                    alert('Wrong URL format!!\nFor example \'http://yourhost.somewhere.com/yourapp\'.');
                    frm.url.focus();
                    return false;
                }

                // check time format
                var regexp = "^(\\d{1}|(0|1)\\d{1}|2[0-3]):(([0-5]\\d{1})|\\d{1})$";
                var match = frm.sendtime.value.match(regexp);
                if (!frm.sendtime.value.test(regexp)) {
                    alert('Time format error!');
                    frm.sendtime.focus();
                    frm.sendtime.select();
                    return false;
                }

                // padding hour
                var regexp = "^\\d{1}:(([0-5]\\d{1})|\\d{1})$";
                if (frm.sendtime.value.test(regexp)) {
                    frm.sendtime.value = "0" + frm.sendtime.value;
                }

                // check date format
                if (getElement('schedule2').checked && frm.senddate.value == "") {
                    alert('Date format error!');
                    frm.senddate.focus();
                    frm.senddate.select();
                    return false;
                }

                // deliver datetime
                frm.deliver_dt.value = (getElement('schedule1').checked)?"NOW()":frm.senddate.value + " " + frm.sendtime.value + ":00";

                // summary oper ID
                var oper_id=0;
                for (var i=1;i<operIdArry.length;i++) {
                    oper_id += (getElement('oper' + i))?(getElement('oper' + i).checked?Math.pow(2, operIdArry[i]):0):0;
                }
                if (oper_id==0) {
                    alert('Please select at least 1 operator!');
                    return false;
                }
                frm.operid.value = oper_id;

                // leave it out when production
                //alert('OK');
                //return false;
            }
        </script>

        <script src='./js/mootools.js' type='text/javascript'></script>
        <script src='./js/calendar.rc4.js' type='text/javascript'></script>
        <script>
            window.onload=function(){
                var frm = document.pushMessageWapFrm;
                updateServiceOptions(0, frm.srvc_main_id);

                if(NiftyCheck())RoundedTop("div#content_header","#C0CDF2","#FFF");
                if(NiftyCheck())RoundedBottom("div#content","#C0CDF2","#377CB1");
                if(NiftyCheck())Rounded("div#content2","#377CB1","#FFF");

                var obj = getElement('srvc_main_id');
                updateOperSelect(obj.value);
                if (!obj.disabled) {
                    getServiceDetails(obj.value, whichOper());
                }
                frm.submit.disabled = obj.disabled;
                frm.cancel.disabled = obj.disabled;

                updateSendtimeBox();

                obj.onchange = function() {
                    updateOperSelect(obj.value);
                    getServiceDetails(obj.value, whichOper());
                }
            }
            window.addEvent('domready', function() { myCal = new Calendar({ senddate: 'Y-m-d' }, { classes: ['dashboard'], direction: .5 }); });
        </script>
    </head>
    <body class="content">
        <div id="pushMessageSmsText" style="width:100%;">
            <form target="ctFrame" name="pushMessageWapFrm" method="post" action="pushMessageSmsServlet"
                  onsubmit="return _onsubmit();">
<%
    if (!_txqid.isEmpty())
        out.print("<input type='hidden' name='txqid' value='" + _txqid + "'>");
%>
                <input type="hidden" name="to" value="ies" >
                <input type="hidden" name="type" value="wap" >
                <input type="hidden" name="deliver_dt" value="">
                <input type="hidden" name="operid" value="">
                <div id="content_header" style="width:75%; background-color:#FFF">
                    <h2 class="smswap">Wap Push</h2>
                </div>
                <div id="content" style="width:75%;padding-top: 10px;">
                    <div id="content2" style="width:90%; text-align:center; background-color:#FFF; margin:10px 0 0 10px;">
                        <table align="center" class="table4" style="width:90%; margin-top:0px; padding:0;">
                            <tr>
                                <th style="border-bottom:0px" width="30%">Service Name :</th>
                                <td>
                                    <script>document.write(createOptions(null, null, 'srvc_main_id', false, 0));</script>
                                </td>
                            </tr>
                            <tr>
                                <th style="border-bottom:0px;"></th>
                                <td>
                                    <div id="service_info_box" style="padding:1px"></div>
                                </td>
                            </tr>
                            <tr>
                                <th>Operator :</th>
                                <td>
                                    <div id="operSelectDiv" class="txt10"></div>
                                </td>
                            </tr>
                            <tr>
                                <th style="border-bottom:0px" colspan="2">WAP details:</th>
                            </tr>
                            <tr>
                                <th style="border-bottom:0px;text-align:right">Url :</th>
                                <td>
                                    <input type="text" name="url" style="width:300px;" value="<%=_url%>">
                                </td>
                            </tr>
                            <tr>
                                <th style="text-align:right">Title :</th>
                                <td>
                                    <input type="text" name="title" style="width:300px;" value="<%=_title%>">
                                </td>
                            </tr>
                            <tr><td></td><td><div id="messageLen"></div></td></tr>
                            <tr>
                                <th style="border-bottom:0px">Deliver Date/Time :</th>
                                <td>
                                    <input type="radio" id="schedule1" name="schedule" value="0" onclick="updateSendtimeBox()" <%=_now%>>
                                    <label for="schedule1"><span style="vertical-align:baseline;">Now</span></label><br>
                                    <input type="radio" id="schedule2" name="schedule" value="1" onclick="updateSendtimeBox()" <%=_schedule%>>
                                    <label for="schedule2"><span style="vertical-align:baseline;">At Tiime :</span></label>

                                    <input type="text" name="senddate" id="senddate" class="calendar" style="width:70px" value="<%=_date%>" disabled>
                                    <script>
                                        document.write("<input type='text' name='sendtime' id='sendtime'");
                                        if ("<%=_time%>" == "")
                                            document.write("value='" + hh + ":" + ((mn<10)?"0":"") + mn + "'");
                                        else
                                            document.write("value='<%=_time%>'");
                                        document.write("maxlength=5 style='width:40px' onkeypress='return filter_digit_char(event, \":\")' disabled> (HH:MM)");
                                    </script>
                                </td>
                            </tr>
                        </table>
                    </div>
                    <div style="margin: 20px 0 20px 50px;">
                        <input id="submit" type="submit" class="button" value="Send">
                        <input id="cancel" type="reset" class="button" value="Cancel">
                    </div>
                </div>
            </form>
        </div>
    </body>
</html>
