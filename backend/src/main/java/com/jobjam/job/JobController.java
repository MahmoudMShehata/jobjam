package com.jobjam.job;

import com.anthropic.models.messages.Base64ImageSource;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private static final int MAX_SCREENSHOTS = 8;
    private static final Map<String, Base64ImageSource.MediaType> SCREENSHOT_TYPES = Map.of(
            "image/png", Base64ImageSource.MediaType.IMAGE_PNG,
            "image/jpeg", Base64ImageSource.MediaType.IMAGE_JPEG,
            "image/webp", Base64ImageSource.MediaType.IMAGE_WEBP,
            "image/gif", Base64ImageSource.MediaType.IMAGE_GIF);

    private final JobRepository jobs;
    private final JobExtractor extractor;

    public JobController(JobRepository jobs, JobExtractor extractor) {
        this.jobs = jobs;
        this.extractor = extractor;
    }

    public record ExtractRequest(String url, String postingText) {
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

    @PostMapping("/extract")
    public JobExtractor.JobDraft extract(@RequestBody ExtractRequest request) {
        return extractor.extract(request.url(), request.postingText(), List.of());
    }

    @PostMapping(path = "/extract/screenshots", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JobExtractor.JobDraft extractFromScreenshots(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String url) {
        if (files.isEmpty() || files.size() > MAX_SCREENSHOTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Upload between 1 and " + MAX_SCREENSHOTS + " screenshots");
        }
        List<JobExtractor.Screenshot> screenshots = new ArrayList<>();
        for (MultipartFile file : files) {
            Base64ImageSource.MediaType mediaType = SCREENSHOT_TYPES.get(file.getContentType());
            if (mediaType == null) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        file.getOriginalFilename() + " is not a PNG, JPEG, WebP or GIF image");
            }
            try {
                screenshots.add(new JobExtractor.Screenshot(mediaType, file.getBytes()));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read " + file.getOriginalFilename());
            }
        }
        return extractor.extract(url, null, screenshots);
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
