package com.dellybizz.notificationvault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DeviceProfile {
    final String id;
    final String name;
    final String type;
    final boolean canRecord;
    final List<ViewSource> viewSources;

    DeviceProfile(String id, String name, String type, boolean canRecord, List<ViewSource> viewSources) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.canRecord = canRecord;
        this.viewSources = Collections.unmodifiableList(new ArrayList<>(viewSources));
    }

    static final class ViewSource {
        final String id;
        final String name;
        final String type;

        ViewSource(String id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }
}
