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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiBucket;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.callback.KiiQueryCallBack;
import com.kii.cloud.storage.query.KiiQuery;
import com.kii.cloud.storage.query.KiiQueryResult;
import com.kii.cloud.storage.resumabletransfer.InvalidHolderException;
import com.kii.cloud.storage.resumabletransfer.KiiDownloader;
import com.kii.cloud.storage.resumabletransfer.KiiUploader;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class MainActivity extends Activity {

	private static final int SELECT_PHOTO = 100;

	private static final String TAG = "MainActivity";

	// define some strings used for creating objects
	private static final String BUCKET_NAME = "FileObjects";

	// define the UI elements
	private ProgressDialog mProgress;

	// define the list
	private ListView mListView;

	// define the list manager
	private ObjectAdapter mListAdapter;

	// define a custom list adapter to handle KiiObjects
	public class ObjectAdapter extends ArrayAdapter<KiiObject> {

		// define some vars
		int resource;
		String response;
		Context context;

		// initialize the adapter
		public ObjectAdapter(Context context, int resource, List<KiiObject> items) {
			super(context, resource, items);

			// save the resource for later
			this.resource = resource;
		}

		@Override
		/**
		 * List row layout
		 */
		public View getView(int position, View convertView, ViewGroup parent) {

			// create the view
			LinearLayout rowView;

			// get a reference to the object
			KiiObject kiiObject = getItem(position);

			// if it's not already created
			if (convertView == null) {

				// create the view by inflating the xml resource
				// (res/layout/row.xml)
				rowView = new LinearLayout(getContext());
				String inflater = Context.LAYOUT_INFLATER_SERVICE;
				LayoutInflater vi;
				vi = (LayoutInflater) getContext().getSystemService(inflater);
				vi.inflate(resource, rowView, true);

			}

			// it's already created, reuse it
			else {
				rowView = (LinearLayout) convertView;
			}

			// get the text fields for the row
			TextView titleText = (TextView) rowView.findViewById(R.id.title);
			titleText.setText(kiiObject.getString("imageName"));

			TextView titleCreated = (TextView) rowView
					.findViewById(R.id.dateCreated);
			titleCreated.setText(new Date(kiiObject.getCreatedTime()).toString());

			// show thumbnail instead of the full image in the list view
			ImageView image = (ImageView) rowView.findViewById(R.id.list_image);

			byte[] bmpBytes = kiiObject.getByteArray("thumbnail");
			Bitmap bmp = BitmapFactory.decodeByteArray(bmpBytes, 0,
					bmpBytes.length);
			image.setImageBitmap(bmp);
			// return the row view
			return rowView;
		}

	}

	/**
	 * the user can add items from the options menu. create that menu here -
	 * from the res/menu/menu.xml file
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Call addItem from the options menu
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		MainActivity.this.addItem(null);
		return true;
	}

	// resize the image for thumbnail
	private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

		// Decode image size
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(
				getContentResolver().openInputStream(selectedImage), null, o);

		// Set thumbnail size
		final int REQUIRED_SIZE = 100;

		// Find the correct scale value. It should be the power of 2.
		int width_tmp = o.outWidth, height_tmp = o.outHeight;
		int scale = 1;
		while (true) {
			if (width_tmp / 2 < REQUIRED_SIZE && height_tmp / 2 < REQUIRED_SIZE) {
				break;
			}
			width_tmp /= 2;
			height_tmp /= 2;
			scale *= 2;
		}

		// Decode with inSampleSize, which is the scaling value
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;
		return BitmapFactory.decodeStream(
				getContentResolver().openInputStream(selectedImage), null, o2);

	}

	/**
	 * helper to retrieve the path of an image URI
	 */
	public String getPath(Uri uri) {
		// just some safety built in
		if (uri == null) {
			// TODO perform some logging or show user feedback
			return null;
		}
		// try to retrieve the image from the media store first
		// this will only work for images selected from gallery
		String[] filePathColumn = { MediaStore.Images.Media.DATA };

		Cursor cursor = getContentResolver().query(uri, filePathColumn, null,
				null, null);
		cursor.moveToFirst();

		int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
		String filePath = cursor.getString(columnIndex);
		cursor.close();
		return filePath;
	}

	@Override
	/**
	 * image selection is done
	 */
	protected void onActivityResult(int requestCode, int resultCode,
			Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

		switch (requestCode) {
		case SELECT_PHOTO:
			if (resultCode == RESULT_OK) {
				Uri selectedImage = imageReturnedIntent.getData();
				// Get reference to the local file
				String selectedImagePath = getPath(selectedImage);
				File imageFile = new File(selectedImagePath);
				String imageName = selectedImagePath
						.substring(selectedImagePath.lastIndexOf("/") + 1);
				try {
					// Create thumbnail
					Bitmap thumbnail = decodeUri(selectedImage);
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					// compress to lossless PNG format with 100% quality
					thumbnail.compress(Bitmap.CompressFormat.PNG, 100, stream);
					byte[] byteArray = stream.toByteArray();
					
					createObject(imageFile, imageName, byteArray);
				} catch (FileNotFoundException e) {
					Log.v(TAG, "Error registering: " + e.getLocalizedMessage());
					Toast.makeText(
							MainActivity.this,
							"Error selecting an image: "
									+ e.getLocalizedMessage(),
							Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	/**
	 * "Add Item" button was clicked on the front end
	 * 
	 * @param v
	 *            - owner view
	 */
	public void addItem(View v) {
		// First select an image
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, SELECT_PHOTO);
	}

	/**
	 * 
	 * @param imageFile
	 *            - Local image file
	 * @param imageName
	 *            - image name to help identify the image
	 * @param thumbnail
	 *            - Bitmap object containing image's thumbnail
	 */
	private void createObject(File imageFile, String imageName, byte[] thumbnail) {
		// show a progress dialog to the user
		mProgress = ProgressDialog.show(MainActivity.this, "",
				"Creating Object...", true);

		// Create an object 
		KiiObject object = Kii.bucket(BUCKET_NAME).object();

		// Set key-value pairs.
		object.set("imageName", imageName);
		object.set("thumbnail", thumbnail);
		// Upload the file.
		KiiUploader uploader = object.uploader(getApplicationContext(), 
		        imageFile);
		try {
		  uploader.transfer(null);
		} catch (Exception e) {
		  // Can be different exceptions - see  http://documentation.kii.com/en/guides/android/managing-data/object-storages/uploading/
			Toast.makeText(MainActivity.this, "Error uploading file",
					Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Error uploading file: " + e.getLocalizedMessage());
			mProgress.dismiss();
			return;
		}
		
		// tell the console and the user it was a success!
		Toast.makeText(MainActivity.this, "Image file saved",
				Toast.LENGTH_SHORT).show();
		Log.d(TAG, "Image file saved: " + imageName);
		
		// insert this object into the beginning of the list adapter
		MainActivity.this.mListAdapter.insert(object, 0);

		mProgress.dismiss();
	}

	/**
	 * Load all existing objects in this bucket from the server. This
	 * is done on view creation
	 */
	private void loadObjects() {

		// default to an empty adapter
		mListAdapter.clear();

		// show a progress dialog to the user
		mProgress = ProgressDialog.show(MainActivity.this, "", "Loading...",
				true);

		// create an empty KiiQuery (will retrieve all results)
		KiiQuery query = new KiiQuery();
		
		// define the bucket to query
		KiiBucket bucket =Kii.bucket(BUCKET_NAME);

		// perform the query
		bucket.query(new KiiQueryCallBack<KiiObject>() {

			// catch the callback's "done" feedback
			public void onQueryCompleted(int token,
					KiiQueryResult<KiiObject> result, Exception e) {

				// hide our progress UI element
				mProgress.dismiss();

				// check for an exception (successful request if e==null)
				if (e == null) {

					// add the objects to the adapter (adding to the listview)
					List<KiiObject> fileLists = result.getResult();
					for (KiiObject kiiObject : fileLists) {
						mListAdapter.add(kiiObject);
					}

					// tell the console and the user it was a success!
					Log.v(TAG, "Images loaded: "
							+ result.getResult().size());
					Toast.makeText(MainActivity.this, "Images loaded",
							Toast.LENGTH_SHORT).show();

				}

				// otherwise, something bad happened in the request
				else {

					// tell the console and the user there was a failure
					Log.v(TAG,
							"Error loading objects: " + e.getLocalizedMessage());
					Toast.makeText(
							MainActivity.this,
							"Error loading objects: " + e.getLocalizedMessage(),
							Toast.LENGTH_SHORT).show();
				}
			}
		}, query);

	}

	/**
	 * Delete button was pressed on the front-end
	 * 
	 * @param position
	 *            - object position in the list
	 */
	void performDelete(int position) {

		// show a progress dialog to the user
		mProgress = ProgressDialog.show(MainActivity.this, "",
				"Deleting object...", true);

		// get the object to delete based on the position of the row in the list
		final KiiObject kiiObject = MainActivity.this.mListAdapter
				.getItem(position);

		// delete the object including object body
		// Alternatively, object body can be deleted using 
		// kiiObject.deleteBody() - this will leave the object itself intact
		//

		try {
			kiiObject.delete();
		} catch (Exception e) {
			// tell the console and the user there was a failure
			Toast.makeText(MainActivity.this, "Error deleting image",
					Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Error deleting image: " + e.getLocalizedMessage());
			mProgress.dismiss();
			return;
		}

		// tell the console and the user it was a success!
		Toast.makeText(MainActivity.this, "Deleted image", Toast.LENGTH_SHORT)
				.show();
		Log.d(TAG, "Deleted image." );

		// remove the object from the list adapter
		MainActivity.this.mListAdapter.remove(kiiObject);
		mProgress.dismiss();
	}

	/**
	 * Called when the user clicks "Full Screen" button on a selected image row
	 * 
	 * @param v
	 *            - owner view
	 */
	public void fullScreenImage(View v) {
		// identify row in the list
		final int position = MainActivity.this.mListView
				.getPositionForView((View) v.getParent());

		// get the file
		final KiiObject kiiObject = MainActivity.this.mListAdapter
				.getItem(position);
		String newFileName = "";
		try {
			// Refresh the instance to get the metadata (if necessary)
			kiiObject.refresh();
			// Download File body if does not exist
			newFileName = "/storage/sdcard/Pictures/" + kiiObject.getString("imageName");
			File localFile = new File(newFileName);
			if (!localFile.exists()) {
				// Create a KiiDownloader.
				KiiDownloader downloader = null;
				try {
				  downloader = kiiObject.downloader(getApplicationContext(), new File(
				        Environment.getExternalStorageDirectory(), newFileName));
				} catch (InvalidHolderException e) {
				    // Target Object has not been saved or is deleted.
					Toast.makeText(MainActivity.this, "Error when trying to download file",
							Toast.LENGTH_SHORT).show();
					Log.d(TAG, "Error when trying to download file: " + e.getLocalizedMessage());
					return;
				}

				// Start downloading.
				try {
				  downloader.transfer(null);
				} catch (Exception e) { 
					// different exceptions possible: http://documentation.kii.com/en/guides/android/managing-data/object-storages/downloading/
					Toast.makeText(MainActivity.this, "Error downloading file",
							Toast.LENGTH_SHORT).show();
					Log.d(TAG, "Error downloading file: " + e.getLocalizedMessage());
					return;
				}
			} 
		} catch (Exception e) {
			// tell the console and the user there was a failure
			Toast.makeText(MainActivity.this, "Error downloading file",
					Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Error downloading file: " + e.getLocalizedMessage());
			return;
		}
		// go to the image screen
		Intent myIntent = new Intent(MainActivity.this, ImageActivity.class);
		myIntent.putExtra("filePath", newFileName);
		MainActivity.this.startActivity(myIntent);

	}

	/**
	 * Called when the user clicks "Delete" button on a selected image row
	 * 
	 * @param v
	 *            - owner view
	 */
	public void deleteImage(View v) {

		// identify row in the list
		final int position = MainActivity.this.mListView
				.getPositionForView((View) v.getParent());

		// build the alert
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Would you like to remove this item? ")
				.setCancelable(true)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {

							// if the user chooses 'yes',
							public void onClick(DialogInterface dialog, int id) {

								// perform the delete action on the selected
								// image
								MainActivity.this.performDelete(position);
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {

					// if the user chooses 'no'
					public void onClick(DialogInterface dialog, int id) {

						// simply dismiss the dialog
						dialog.cancel();
					}
				});

		// show the dialog
		builder.create().show();

	}

	@Override
	/**
	 * Screen initialization
	 */
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// set our view to the xml in res/layout/main.xml
		setContentView(R.layout.main);

		// create an empty list adapter
		mListAdapter = new ObjectAdapter(this, R.layout.row,
				new ArrayList<KiiObject>());

		mListView = (ListView) this.findViewById(R.id.list);

		// set it to our view's list
		mListView.setAdapter(mListAdapter);

		// query for any previously-created objects
		this.loadObjects();
		
	}

}
