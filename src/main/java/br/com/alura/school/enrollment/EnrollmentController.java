package br.com.alura.school.enrollment;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.alura.school.course.CourseRepository;
import br.com.alura.school.user.UserRepository;


@RestController
public class EnrollmentController {
    
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public EnrollmentController(EnrollmentRepository enrollmentRepository, CourseRepository courseRepository, 
    UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/courses/enroll/report")
    public ResponseEntity<List<EnrollmentReportResponse>> enrollReport() {
        var users = userRepository.findAll();
        List<EnrollmentReportResponse> enrollmentReport = new ArrayList<>();

        users.forEach(user -> {
            var enrollments = enrollmentRepository.findByUser(user);
            if(!enrollments.isEmpty()) {
                enrollmentReport.add(new EnrollmentReportResponse(user.getEmail(), enrollments.size()));
            }
        });
        
        if (enrollmentReport.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        enrollmentReport.sort(Comparator.comparing(EnrollmentReportResponse::getEnrollmentQuantity).reversed());
        return ResponseEntity.ok(enrollmentReport);
    }

    @PostMapping("/courses/{courseCode}/enroll")
    public ResponseEntity<Void> newEnrollment(@RequestBody @Valid NewEnrollmentRequest request, @PathVariable("courseCode") String courseCode) {
        var course  = courseRepository
            .findByCode(courseCode)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, format("Course with code %s not found", courseCode)));

        var user = userRepository
            .findByUsername(request.getUsername())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, format("User with username %s not found", request.getUsername())));

        var enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId());
        if (!enrollment.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        enrollmentRepository.save(new Enrollment(LocalDateTime.now(), course, user));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
