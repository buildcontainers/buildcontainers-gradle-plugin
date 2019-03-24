package com.github.buildcontainers.task.docker.container

import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.core.async.ResultCallbackTemplate

class LogContainerCallback(val containerOutputLogger: (msg: String) -> Unit)
    : ResultCallbackTemplate<LogContainerCallback, Frame>() {
    override fun onNext(item: Frame) {
        containerOutputLogger(String(item.payload).trimEnd())
    }
}