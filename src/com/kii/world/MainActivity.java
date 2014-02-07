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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
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

import com.kii.cloud.storage.KiiBucket;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.callback.KiiObjectCallBack;
import com.kii.cloud.storage.callback.KiiQueryCallBack;
import com.kii.cloud.storage.query.KiiQuery;
import com.kii.cloud.storage.query.KiiQueryResult;

public class MainActivity extends Activity {

	private static final int SELECT_PHOTO = 100;

	private static final String TAG = "MainActivity";

	// define some strings used for creating objects
	private static final String BUCKET_NAME = "imageBucket";

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
		public ObjectAdapter(Context context, int resource,
				List<KiiObject> items) {
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
			KiiObject obj = getItem(position);

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
			titleText.setText(obj.getString("imageName"));
			
			TextView titleCreated = (TextView) rowView.findViewById(R.id.dateCreated);
			titleCreated.setText(new Date(obj.getCreatedTime()).toString());

			// set the image
			ImageView image = (ImageView) rowView.findViewById(R.id.list_image);

			byte[] bmpBytes = obj.getByteArray("image");
			Bitmap bmp = BitmapFactory.decodeByteArray(bmpBytes, 0,
					bmpBytes.length);
			image.setImageBitmap(bmp);
			// return the row view
			return rowView;
		}

	}

	
	/**
	 * the user can add items from the options menu.
	 * create that menu here - from the res/menu/menu.xml file
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Call addItem from the otions menu
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		MainActivity.this.addItem(null);
		return true;
	}

	// resize the image to fit in Kii object
	private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

		// Decode image size
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(
				getContentResolver().openInputStream(selectedImage), null, o);

		// We can return the result of decodeStream function above. However, it
		// may be worth scaling the images so that they are not bigger than certain 
		// size. This ensures that the image fits on the screen and is small enough
		// to be saved in Kii cloud object
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
				String imageName = selectedImage.getPath();
				try {
					Bitmap yourSelectedImage = decodeUri(selectedImage);
					createObject(yourSelectedImage, imageName);
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
	 * @param image
	 *            - Bitmap object containing selected image
	 * @param imageName
	 *            - image name to help identify the image
	 */
	private void createObject(Bitmap image, String imageName) {
		// show a progress dialog to the user
		mProgress = ProgressDialog.show(MainActivity.this, "",
				"Creating Object...", true);

		// convert image to byte array
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		// compress to lossless PNG format
		image.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] byteArray = stream.toByteArray();

		// get a reference to a KiiBucket
		KiiBucket bucket = KiiUser.getCurrentUser().bucket(BUCKET_NAME);

		// create a new KiiObject and set image and imageName keys
		KiiObject obj = bucket.object();
		obj.set("imageName", imageName);
		obj.set("image", byteArray);

		// save the object asynchronously
		obj.save(new KiiObjectCallBack() {

			// catch the callback's "done" request
			public void onSaveCompleted(int token, KiiObject o, Exception e) {

				// hide our progress UI element
				mProgress.dismiss();

				// check for an exception (successful request if e==null)
				if (e == null) {

					// tell the console and the user it was a success!
					Toast.makeText(MainActivity.this, "Created object",
							Toast.LENGTH_SHORT).show();
					Log.d(TAG, "Created object: " + o.toString());

					// insert this object into the beginning of the list adapter
					MainActivity.this.mListAdapter.insert(o, 0);

				}

				// otherwise, something bad happened in the request
				else {

					// tell the console and the user there was a failure
					Toast.makeText(MainActivity.this, "Error creating object",
							Toast.LENGTH_SHORT).show();
					Log.d(TAG,
							"Error creating object: " + e.getLocalizedMessage());
				}

			}
		});

	}

	/**
	 * Load all existing objects associated with this user from the server. This
	 * is done on view creation
	 */
	private void loadObjects() {

		// default to an empty adapter
		mListAdapter.clear();

		// show a progress dialog to the user
		mProgress = ProgressDialog.show(MainActivity.this, "", "Loading...",
				true);

		// create an empty KiiQuery (will retrieve all results, sorted by
		// creation date)
		KiiQuery query = new KiiQuery(null);
		query.sortByAsc("_created");

		// define the bucket to query
		KiiBucket bucket = KiiUser.getCurrentUser().bucket(BUCKET_NAME);

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
					List<KiiObject> objLists = result.getResult();
					for (KiiObject obj : objLists) {
						mListAdapter.add(obj);
					}

					// tell the console and the user it was a success!
					Log.v(TAG, "Objects loaded: "
							+ result.getResult().toString());
					Toast.makeText(MainActivity.this, "Objects loaded",
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
		final KiiObject o = MainActivity.this.mListAdapter.getItem(position);

		// delete the object asynchronously
		o.delete(new KiiObjectCallBack() {

			// catch the callback's "done" request
			public void onDeleteCompleted(int token, Exception e) {

				// hide our progress UI element
				mProgress.dismiss();

				// check for an exception (successful request if e==null)
				if (e == null) {

					// tell the console and the user it was a success!
					Toast.makeText(MainActivity.this, "Deleted object",
							Toast.LENGTH_SHORT).show();
					Log.d(TAG, "Deleted object: " + o.toString());

					// remove the object from the list adapter
					MainActivity.this.mListAdapter.remove(o);

				}

				// otherwise, something bad happened in the request
				else {

					// tell the console and the user there was a failure
					Toast.makeText(MainActivity.this, "Error deleting object",
							Toast.LENGTH_SHORT).show();
					Log.d(TAG,
							"Error deleting object: " + e.getLocalizedMessage());
				}

			}
		});
	}

	
	/**
	 * Called when the user clicks "Delete" button on a selected image row
	 * @param v - owner view
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

								// perform the delete action on the selected image
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
