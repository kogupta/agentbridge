package com.github.catatafishen.agentbridge.nativeagent.run;

import java.util.List;

@FunctionalInterface
public interface ModelItemRenderer {
    List<CacheField> render(RunMessage message, int itemIndex);
}
