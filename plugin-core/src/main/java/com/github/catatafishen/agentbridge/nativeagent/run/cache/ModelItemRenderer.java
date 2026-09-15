package com.github.catatafishen.agentbridge.nativeagent.run.cache;

import java.util.List;
import com.github.catatafishen.agentbridge.nativeagent.run.session.RunMessage;

@FunctionalInterface
public interface ModelItemRenderer {
    List<CacheField> render(RunMessage message, int itemIndex);
}
