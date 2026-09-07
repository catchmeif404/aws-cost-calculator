package com.awscalculator.backend.pricing;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private static final List<RegionResponse> REGIONS = List.of(
            new RegionResponse("ap-northeast-2", "서울 (ap-northeast-2)"),
            new RegionResponse("ap-northeast-1", "도쿄 (ap-northeast-1)"),
            new RegionResponse("us-east-1", "버지니아 (us-east-1)"),
            new RegionResponse("us-west-2", "오레곤 (us-west-2)"),
            new RegionResponse("eu-central-1", "프랑크푸르트 (eu-central-1)")
    );

    @GetMapping
    public List<RegionResponse> list() {
        return REGIONS;
    }
}
