package com.sms.dto;

import com.sms.model.TimetableHoliday;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimetableHolidayDTO {
    
    private Long id;
    private LocalDate holidayDate;
    private TimetableHoliday.HolidayType holidayType;
    private String reason;
    private String description;
    private String dayName;
    
    public String getDayName() {
        if (holidayDate != null) {
            return holidayDate.getDayOfWeek().name();
        }
        return null;
    }
}
