package com.jobjam.job;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobRepository jobs;

    public JobController(JobRepository jobs) {
        this.jobs = jobs;
    }

    @GetMapping
    public List<Job> list() {
        return jobs.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public Job get(@PathVariable Long id) {
        return jobs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Job create(@Valid @RequestBody JobRequest request) {
        return jobs.save(request.toEntity());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!jobs.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        jobs.deleteById(id);
    }
}
