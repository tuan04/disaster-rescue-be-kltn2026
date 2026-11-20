package iuh.fit.dispatchservice.dtos.projection;
import java.time.LocalDate;

public interface RescueStatisticsProjection {

    LocalDate getPeriodStart();

    Long getCompleted();

    Long getSafe();

    Long getPending();

}
