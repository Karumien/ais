package com.karumien.cloud.ais.api;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.karumien.cloud.ais.api.handler.WorkApi;
import com.karumien.cloud.ais.api.model.UserInfoDTO;
import com.karumien.cloud.ais.api.model.WorkDTO;
import com.karumien.cloud.ais.api.model.WorkDayDTO;
import com.karumien.cloud.ais.api.model.WorkDayTypeDTO;
import com.karumien.cloud.ais.api.model.WorkHourDTO;
import com.karumien.cloud.ais.api.model.WorkMonthDTO;
import com.karumien.cloud.ais.api.model.WorkTypeDTO;
import com.karumien.cloud.ais.service.AISService;

/**
 * REST API for AIS Services.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 18:54:23
 */
@RestController
@RequestMapping(path = "/api")
public class AISWorkRestController implements WorkApi {
    
    private static final String ATTACHMENT_FILENAME = "attachment; filename=";

    private static final String CONTENT_DISPOSITION = "Content-disposition";
//    private static final String CONTENT_TYPE = "content-type";

    /** MediaType Application Excel Openformat */
    private static final String APPLICATION_EXCEL_VALUE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    
    @Autowired
    private ModelMapper mapper;
    
    @Autowired
    private AISService aisService;
    
    @Value(value = "${jsp.redirect:false}")
    private Boolean redirect;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<WorkMonthDTO> getWorkDays(@NotNull @Valid String username, @Valid Integer year,
            @Valid Integer month) {
        return new ResponseEntity<>(
                mapper.map(aisService.getWorkDays(year, month, username), WorkMonthDTO.class), HttpStatus.OK);
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<UserInfoDTO>> getWorkUsers(@Valid String username) {
        return new ResponseEntity<>(aisService.getWorkUsers(username), HttpStatus.OK);
    }
    

    /**
     * GET /work/export/xls : Generate export workdays
     *
     * @param response
     *            {@link HttpServletResponse}
     * @throws IOException
     *             on IO error
     */
    @RequestMapping(value = "/work/export", produces = { APPLICATION_EXCEL_VALUE }, method = RequestMethod.POST)
    public void exportWorkDays(@NotNull @Valid @RequestParam(value = "role", required = true) String role,
            @Valid @RequestParam(value = "username", required = false) String username,
            @Valid @RequestParam(value = "month", required = false) Integer month, 
            @Valid @RequestParam(value = "year", required = false) Integer year,
            HttpServletResponse response) throws IOException {

        if (year == null) {
            year = LocalDate.now().getYear();
        }

        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }

        if (username == null) {
            username = role;
        }        

        String yearmonth = year + "." + (month < 10 ? "0" : "") + month;
        response.setHeader(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + yearmonth + "-" + username + ".xlsx");
        aisService.exportWorkDays(year, month, username, response.getOutputStream());
    }

    /**
     * HTML formated Users on site.
     * 
     * @deprecated will be replaced by UI application
     * @return HTML table of Users on site
     */
    @RequestMapping(value = "/work/html", produces = { "text/html" }, method = RequestMethod.GET)
    @Deprecated
    public String getUserMonthHTML(@NotNull @Valid @RequestParam(value = "role", required = true) String role,
            @Valid @RequestParam(value = "username", required = false) String username,
            @Valid @RequestParam(value = "month", required = false) Integer month, 
            @Valid @RequestParam(value = "year", required = false) Integer year) {
        
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }
        
        if (username == null) {
            username = role;
        }        

        LocalDate actualMonthDay = LocalDate.now();
        boolean currentMonth = (actualMonthDay.getYear() == year && actualMonthDay.getMonthValue() == month);

        StringBuilder sb = new StringBuilder("<script type=\"text/javascript\">"
        + "function updateWork(form, username) {"
        + "  var xhttp = new XMLHttpRequest();"
        + "  xhttp.open(\"POST\", \""+ (Boolean.TRUE.equals(redirect) ? "" : "http://192.168.2.222:2222") + "/api/work/update?username=" 
        + "\"+username, true);"
        + "  xhttp.setRequestHeader(\"Content-type\", \"application/json\");"
        + "  if (form.hours.value && form.workType.value != 'NONE' && form.hours2.value && form.workType2.value != 'NONE'"
        + "  ||  !form.hours.value && form.workType.value == 'NONE' && form.hours2.value && form.workType2.value != 'NONE'"
        + "  ||  form.hours.value && form.workType.value != 'NONE' && !form.hours2.value && form.workType2.value == 'NONE'"
        + "  ||  !form.hours.value && form.workType.value == 'NONE' && !form.hours2.value && form.workType2.value == 'NONE'"
        + ") {"
        + "  xhttp.send('{ \"id\": ' + form.id.value + '," 
        + "        \"hours\": ' + (!form.hours.value ? null : form.hours.value.replace(',','.')) + ',"
        + "        \"hours2\": ' + (!form.hours2.value ? null : form.hours2.value.replace(',','.')) + ',"
        + "        \"workType\": \"' + form.workType.value + '\","
        + "        \"workType2\": \"' + form.workType2.value + '\" }');"
        + "  }}</script>");

        sb.append("<table cellspacing=\"5\" class=\"aditus\"><form action=\""+ 
                (Boolean.TRUE.equals(redirect) ? "/api/work/html" : "/ais.jsp" ) + "\" method=\"get\">");
        sb.append("<tr><td colspan=\"7\"><select name=\"month\" class=\"unvisiblelines\" onchange=\"this.form.submit()\">");
        
        List<String> months = Arrays.asList("leden", "únor", "březen", "duben", "květen", "červen", 
                "červenec", "srpen", "září", "říjen", "listopad", "prosinec");
        for (int i = 4; i < 12; i++) {
            sb.append("<option value=\"").append(i+1).append("\"").append(month.equals(i+1) ? " selected" : "");
            sb.append(">").append(months.get(i)).append("</option>");
        }
        sb.append("</select><select class=\"unvisiblelines\"><option selected>2019</select><input type=\"hidden\" name=\"role\" value=\"").append(role).append("\">");
        
        UserInfoDTO selectedUser = mapper.map(aisService.getUser(username), UserInfoDTO.class);
        UserInfoDTO roleUser = mapper.map(aisService.getUser(role), UserInfoDTO.class);

        sb.append("</td>");
        sb.append("<td align=\"right\"><select class=\"unvisiblelines\" name=\"username\" onchange=\"this.form.submit()\">");
                            
        for (UserInfoDTO user : aisService.getWorkUsers(role)) {
            sb.append("<option value=\"").append(user.getUsername()).append("\"").append(username.equals(user.getUsername()) ? " selected" : "");
            sb.append(">").append(user.getName()).append("</option>");            
        }
        
        sb.append("</select></td></tr></form>");                
        
        sb.append("<form id=\"exportForm\" action=\"" + (Boolean.TRUE.equals(redirect) ? "" : "http://192.168.2.222:2222") + "/api/work/export?username=" 
                +username+"&role="+role+"&year="+year+"&month="+month + "\" method=\"post\">");
        sb.append("<tr>");
        sb.append("<td class=\"i24_tableHead menuline\" align=\"right\">Datum</td>"
            + "<td class=\"i24_tableHead menuline\" align=\"right\">Kategorie</td>"
            + "<td class=\"i24_tableHead menuline\">Příchod</td>"
            + "<td class=\"i24_tableHead menuline\" align=\"center\">Oběd od-do</td>"
            + "<td class=\"i24_tableHead menuline\">Odchod</td>"
            + "<td class=\"i24_tableHead menuline\" align=\"right\">Celkem</td><td>&nbsp;</td>"
            + "<td class=\"i24_tableHead menuline\" style=\"text-align: right\">Výkazy (").append(username).append(")");

        if (roleUser.isRoleAdmin()) {
            sb.append("<a href=\"#\" onclick=\"document.getElementById('exportForm').submit();\">");
            sb.append("<img onclick=\"this.form.submit();\" src=\"/img/printer.gif\" style=\"position: relative; top: 4px; margin-left: 6px; width: 15px; height: 16px;\" border=\"0\"/></a>");
        }
        sb.append("</td></tr></form>");

        int countWorkDays = 0;
        double fond = selectedUser.getFond() != null ? selectedUser.getFond() / 100d : 1d;
        WorkMonthDTO workMonthDTO = aisService.getWorkDays(year, month, username);
        for (WorkDayDTO workDay : workMonthDTO.getWorkDays()) {
            
            WorkDTO work = workDay.getWork();

            sb.append("<tr>");
            
            if (work != null) {
                sb.append("<form name=\"form"+ work.getId() +"\">");
            }
            
            sb.append("<td class=\"i24_tableItem\"><i>").append(aisService.date(workDay.getDate())).append("</i></td>");
            sb.append("<td class=\"i24_tableItem\">").append(getDescription(workDay.getWorkDayType())).append("</td>");
            sb.append("<td class=\"i24_tableItem\"><b>").append(hoursOnly(workDay.getWorkStart())).append("</b></td>");
            sb.append("<td class=\"i24_tableItem\" align=\"center\">")
                .append(hoursOnly(workDay.getLunchStart())).append(workDay.getLunchStart() != null ? " - " : "").append(hoursOnly(workDay.getLunchEnd())).append("</td>");
            sb.append("<td class=\"i24_tableItem\"><b>").append(hoursOnly(workDay.getWorkEnd())).append("</b></td>");
            sb.append("<td class=\"i24_tableItem\" align=\"right\"><b>").append(aisService.hours(workDay.getWorkedHours())).append("</b></td>");
            
            if (workDay.getDate().getDayOfWeek() != DayOfWeek.SATURDAY
                && workDay.getDate().getDayOfWeek() != DayOfWeek.SUNDAY 
                && workDay.getWorkDayType() != WorkDayTypeDTO.NATIONAL_HOLIDAY) {

                if (currentMonth && actualMonthDay.isAfter(workDay.getDate())) {
                    countWorkDays ++;
                }
                
                if (workDay.getWork() == null) {
                    continue;
                }
                                
                sb.append("<td class=\"i24_tableItem\"><input type=\"hidden\" name=\"id\" value=\""+ work.getId() +"\">"
                        + "<input class=\"unvisiblelines\" onChange=\"updateWork(this.form, '"+username+"')\" type=\"text\" name=\"hours\" style=\"width: 35px; margin-left:10px\" value=\"")
                    .append(work != null ? aisService.hours(work.getHours()) : "")
                    .append("\"><select class=\"unvisiblelines\" name=\"workType\" onChange=\"updateWork(this.form, '"+username+"')\">");
                for (WorkTypeDTO type: WorkTypeDTO.values()) {
                    sb.append("<option value=\"").append(type.name()).append("\"").append(work != null && work.getWorkType() == type ? " selected" : "");
                    sb.append(">").append(aisService.getDescription(type)).append("</option>");
                }
                sb.append("</select></td>");

                sb.append("<td class=\"i24_tableItem\"><input class=\"unvisiblelines\" onChange=\"updateWork(this.form, '"+username+"')\" name=\"hours2\" type=\"text\" style=\"width: 35px; margin-left:10px\" value=\"")
                    .append(work != null ? aisService.hours(work.getHours2()) : "")
                    .append("\"><select class=\"unvisiblelines\" name=\"workType2\" onChange=\"updateWork(this.form, '"+username+"')\">");
                for (WorkTypeDTO type: WorkTypeDTO.values()) {
                    sb.append("<option value=\"").append(type.name()).append("\"").append(work != null && work.getWorkType2() == type ? " selected" : "");
                    sb.append(">").append(aisService.getDescription(type)).append("</option>");
                }
                sb.append("</select></td>");

            }
                        
            if (work != null) {
                sb.append("</form>");
            }

            sb.append("</tr>");
            
            if (workDay.getDate().getDayOfWeek() == DayOfWeek.SUNDAY && ! workDay.getDate().isEqual(workDay.getDate().with(TemporalAdjusters.lastDayOfMonth()))) {
                sb.append("<tr><td colspan=\"8\"><hr/></td></tr>");
            }
        }
        sb.append("</table>");

        
        StringBuilder sb1 = new StringBuilder("<table cellspacing=\"5\" class=\"aditus\"><tr><td colspan=\"5\"><hr/></td></tr><tr>");
        StringBuilder sb2 = new StringBuilder("<tr>");


        sb1.append("<td class=\"i24_tableItem\"><i>").append("Fond").append("</i></td>");
        sb2.append("<td class=\"i24_tableItem\"><b>").append(
                selectedUser.getFond() == null ? aisService.days(workMonthDTO.getSumWorkDays()) : 
                    aisService.days(workMonthDTO.getSumWorkDays() * fond) + "</b> / " + aisService.days(workMonthDTO.getSumWorkDays())
        ).append("</b></td>");
        
        sb1.append("<td class=\"i24_tableItem\"><i>").append("Svátky").append("</i></td>");
        sb2.append("<td class=\"i24_tableItem\"><b>").append(aisService.days(workMonthDTO.getSumHolidays())).append("</b></td>");

        sb1.append("<td class=\"i24_tableItem\" style=\"#888888\"><i>").append("Aditus").append("</i></td>");
        
        sb2.append("<td class=\"i24_tableItem\"><b>").append(aisService.days(workMonthDTO.getSumOnSiteDays())).append("</b>" + 
                (currentMonth ?  " / " + aisService.days(countWorkDays * fond) : "")
          ).append("</b></td>");
        
        
        for (WorkDTO work : workMonthDTO.getSums()) {
            sb1.append("<td class=\"i24_tableItem\"><i>").append(aisService.getDescription(work.getWorkType())).append("</i></td>");
            sb2.append("<td class=\"i24_tableItem\"><b>")
                    .append(aisService.days(work.getHours() == null ? null : work.getHours() / AISService.HOURS_IN_DAY)).append("</b></td>");
        }
        
        sb2.append("</tr>");
        sb1.append(sb2);
        sb1.append("</tr></table>");
        sb.append(sb1);
        return sb.toString();
    }
    
    private String hoursOnly(@Valid WorkHourDTO work) {
        if (work == null || work.getDate() == null) {
            return "";
        }
        return "<span style=\"color:" + (work.isCorrected() ? "#888888":"#000") +"\">" 
           + aisService.hoursOnly(work) + "</span>";
    }
    
    private String getDescription(@Valid WorkDayTypeDTO workDayType) {
        switch (workDayType) {
        case NATIONAL_HOLIDAY:
            return "<b>Státní svátek</b>";            
        case WORKDAY:
            return "Pracovní den";
        default:
            return "";
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<Void> setWork(@Valid WorkDTO work, @NotNull @Valid String username) {
        aisService.setWork(work, username);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
}
