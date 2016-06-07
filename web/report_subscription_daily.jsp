<%-- 
    Document   : report_subscription
    Created on : 5 ก.พ. 2553, 16:28:39
    Author     : developer
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="hippoping.smsgw.api.db.ServiceElement.SERVICE_TYPE" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link href="./css/cv.css" rel="stylesheet" type="text/css">
        <link href="./css/niftyCorners.css" rel="stylesheet" type="text/css">
        <link href="./css/niftyPrint.css" rel="stylesheet" type="text/css" media="print">
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
                    int srvc_type = 
                            SERVICE_TYPE.SUBSCRIPTION.getId();
        %>
        <jsp:include page="./services_bean.jsp">
            <jsp:param name="srvc_type" value="<%=srvc_type%>" />
        </jsp:include>
        <script type="text/javascript">
            var now = new Date();

            var dd = now.getDate();
            var mm = now.getMonth();
            var yy = now.getFullYear();

            function show_oper(obj) {
                var myOpers = new Array(4); // create Array 2 dimensions
                for (operid in operIdArry) {
                    myOpers[operid] = new Array(operid, operNameArry[operid]);
                }

                // Add options to dropdownlist
                addOption_list(obj, myOpers);
                obj.selectedIndex = 0;
            }

            function updateServiceOptions2(oper_id, obj) {
                var frm = document.forms["reportSubscriptionDailyFrm"];
                removeAllOptions(obj);

                if (!optionsArry[oper_id]) {
                    var tmpArry = new Array(); // create Array 2 dimensions
                    tmpArry[0] = new Array(2);
                    tmpArry[0][0] = "0";
                    tmpArry[0][1] = "No service available";

                    addOption_list(obj, tmpArry);
                    frm.srvcid.disabled = true;
                }
                else {
                    // insert "All Services" at index 0
                    var tmpArry = new Array(2);
                    tmpArry[0] = "0";
                    tmpArry[1] = "All Services";
                    var myArry = optionsArry[oper_id].slice();
                    myArry.unshift(tmpArry);

                    addOption_list(obj, myArry);
                    frm.srvcid.disabled = false;
                }
            }

            function report_subscription_daily_onsubmit() {
                var frm = document.forms["reportSubscriptionDailyFrm"];

                // Date validate
                if (!isValidDate(frm.fday.value, frm.fmonth.value ,frm.fyear.value)) {
                    alert('From date is not valid.');
                    frm.fday.focus();
                    return false;
                }
                if (!isValidDate(frm.tday.value, frm.tmonth.value ,frm.tyear.value)) {
                    alert('To date is not valid.');
                    frm.tday.focus();
                    return false;
                }


                frm.fdate.value = frm.fyear.value + "-" + frm.fmonth.value + "-" + frm.fday.value;
                frm.tdate.value = frm.tyear.value + "-" + frm.tmonth.value + "-" + frm.tday.value;

                // leave it when production
                //alert(frm.fdate.value + " : " + frm.tdate.value);
                //return false;
            }

            window.onload=function(){

                var frm = document.forms["reportSubscriptionDailyFrm"];
                show_oper(frm.operid);
                updateServiceOptions2(frm.operid.options[frm.operid.selectedIndex].value, frm.srvcid);

                frm.operid.onchange=
                    function(){updateServiceOptions2(frm.operid.options[frm.operid.selectedIndex].value, frm.srvcid);}

                frm.fdate.value = frm.fyear.value + "-" + frm.fmonth.value + "-" + frm.fday.value;
                frm.tdate.value = frm.tyear.value + "-" + frm.tmonth.value + "-" + frm.tday.value;
                //frm.submit();

                if(NiftyCheck())Rounded("div#reportSubscriptionDailyContent","#C0CDF2","#FFF");
            }
        </script>
    </head>
    <body class="content">
        <div id="reportSubscriptionDailyContent" style="width:98%; height:450px; background-color:#FFF;">
            <form target="resultFrame" id="reportSubscriptionDailyFrm" method="post" action="ReportSubscriptionDailyServlet" onsubmit="return report_subscription_daily_onsubmit();">
                <input type="hidden" name="cmd" value="refresh">
                <input type="hidden" name="page" value="1">
                <input type="hidden" name="orderby" value="register">
                <input type="hidden" name="swap" value="1">
                <input type="hidden" name="fdate" value="">
                <input type="hidden" name="tdate" value="">
                <div style="padding: 5px 10px 5px 10px;">
                    <h2 class="report">Daily Report</h2><hr>
                    <table class="table4" style="width:100%;">
                        <tr>
                            <th style="border-bottom:0px;"><b>Select Group:</b>
                                <script>
                                    document.write(createOptions(null, null, 'operid', false, 0));
                                    document.write("<b> | </b>");
                                    document.write(createOptions(null, null, 'srvcid', false, 0));
                                </script>
                            </th>
                            <th align="right" style="border-bottom:0px;">Show:
                            <input type="text" name="rows" value="15" style="width:40px;" onkeypress='return filter_digit_char(event)'>
                                Row(s)/Page</th>
                        </tr>
                        <tr>
                            <th style="border-bottom:0px;" colspan="2">From:
                                <script>
                                    showDate(dd, "fday");
                                    document.write(" ");
                                    //showMonth(((mm>0)?mm-1:12), "fmonth");
                                    showMonth(mm, "fmonth");
                                    document.write(" ");
                                    showYear(yy, "fyear");
                                </script>
                                To:
                                <script>
                                    showDate(dd, "tday");
                                    document.write(" ");
                                    showMonth(mm, "tmonth");
                                    document.write(" ");
                                    showYear(yy, "tyear");
                                </script>
                                <input type="submit" class="button" value="Search">
                                <input type="reset" class="button" value="Clear">
                            </th>
                        </tr>
                    </table>
                </div>
            </form>
            <iframe name="resultFrame" frameborder="0" style="overflow:auto; padding:0;width:100%;height:100%"></iframe>
        </div>
    </body>
</html>
