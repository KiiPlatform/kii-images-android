//
//
// Copyright 2014 Kii Corporation
// http://kii.com
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//

package com.kii.world;

import android.app.Application;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.Kii.Site;

public class HelloKii extends Application {
	
    @Override
    public void onCreate() {
        super.onCreate();
                
        // initialize the Kii SDK!
        // Create an Android app at developer.kii.com and enter your own app_id, app_key and backend location here
        Kii.initialize("b05934a0", "318484e25dc7fc27ca254998602baea5", Site.US);
    }
	
}
