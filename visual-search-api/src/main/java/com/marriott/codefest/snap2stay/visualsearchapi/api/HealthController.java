package com.marriott.codefest.snap2stay.visualsearchapi.api;

import com.marriott.codefest.snap2stay.visualsearchapi.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/health")
public class HealthController {

    private final VectorStore vectorStore;

    public HealthController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @GetMapping("/live")
    public Map<String, Object> live() {
        return Map.of("status", "UP");
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> body = new HashMap<>();
        Map<String, String> components = new HashMap<>();

        int size = vectorStore.size();
        components.put("vectorStore", size > 0 ? "UP" : "EMPTY");
        components.put("indexedImages", String.valueOf(size));

        boolean ready = size > 0;
        body.put("status", ready ? "UP" : "DEGRADED");
        body.put("components", components);
        return ResponseEntity.status(ready ? 200 : 503).body(body);
    }
}
