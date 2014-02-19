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

import java.io.File;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

public class ImageActivity extends Activity {

	private ImageView imageView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image);
		// load full-screen image
		imageView = (ImageView) this.findViewById(R.id.full_image);
		String filePath = getIntent().getStringExtra("filePath");
		File imgFile = new  File(filePath);
		
		if(imgFile.exists()){
			Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
		
		    imageView.setImageBitmap(bitmap);
		}
	}


}
