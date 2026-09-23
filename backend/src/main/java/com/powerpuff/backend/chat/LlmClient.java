package com.powerpuff.backend.chat;

import java.util.List;
import java.util.Map;

public interface LlmClient {
    record Action(String action,String query,Long productId,String quantity) {}
    Action route(String text,List<Map<String,String>> history);
}
