/*
 * Copyright 2016 Function1. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tools.gsf.config.inject;

import COM.FutureTense.Interfaces.ICS;
import tools.gsf.time.Stopwatch;

/**
 * @author Tony Field
 * @since 2016-07-21
 */
public class AnnotationInjector implements Injector {

    private final ICS ics;
    private final BindInjector bindInjector;
    private final InjectForRequestInjector ifrInjector;
    private final MappingInjector mappingInjector;
    private final CurrentAssetInjector currentAssetInjector;
    private final Stopwatch stopwatch;

    public AnnotationInjector(ICS ics, BindInjector bind, MappingInjector mapping, InjectForRequestInjector ifr, CurrentAssetInjector currAssetInj, Stopwatch stopwatch) {
        this.ics = ics;
        this.bindInjector = bind;
        this.mappingInjector = mapping;
        this.ifrInjector = ifr;
        this.currentAssetInjector = currAssetInj;
        this.stopwatch = stopwatch;
    }

    @Override
    public void inject(Object dependent) {
        stopwatch.start();

        bindInjector.bind(dependent);
        stopwatch.split("AnnotationInjector: Bind injection done");

        mappingInjector.inject(dependent, ics.GetVar("pagename"));
        stopwatch.split("AnnotationInjector: Mapping injection done");

        ifrInjector.inject(dependent);
        stopwatch.split("AnnotationInjector: InjectForRequest injection done");

        currentAssetInjector.inject(dependent);
        stopwatch.split("AnnotationInjector: CurrentAssetInjector injection done");
    }

}
