/*
Simple DirectMedia Layer
Java source code (C) 2009-2014 Sergii Pylypenko

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:

1. The origin of this software must not be misrepresented; you must not
   claim that you wrote the original software. If you use this software
   in a product, an acknowledgment in the product documentation would be
   appreciated but is not required. 
2. Altered source versions must be plainly marked as such, and must not be
   misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/

package net.sourceforge.clonekeenplus;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.util.Log;
import java.io.*;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.StatFs;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.Collections;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.lang.String;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Button;
import android.widget.Scroller;
import android.view.View;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.text.Editable;
import android.text.SpannedString;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.util.DisplayMetrics;
import android.net.Uri;
import java.util.concurrent.Semaphore;
import java.util.Arrays;
import android.graphics.Color;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.hardware.Sensor;
import android.widget.Toast;
import android.text.InputType;


class SettingsMenuMisc extends SettingsMenu
{
	static class DownloadConfig extends Menu
	{
		String title(final MainActivity p)
		{
			return p.getResources().getString(R.string.storage_question);
		}
		void run (final MainActivity p)
		{
			long freeSdcard = 0;
			long freePhone = 0;
			try
			{
				StatFs phone = new StatFs(p.getFilesDir().getAbsolutePath());
				freePhone = (long)phone.getAvailableBlocks() * phone.getBlockSize() / 1024 / 1024;
				StatFs sdcard = new StatFs(Settings.SdcardAppPath.get().bestPath(p));
				freeSdcard = (long)sdcard.getAvailableBlocks() * sdcard.getBlockSize() / 1024 / 1024;
			}
			catch(Exception e) {}

			final CharSequence[] items = { p.getResources().getString(R.string.storage_phone, freePhone),
											p.getResources().getString(R.string.storage_sd, freeSdcard),
											p.getResources().getString(R.string.storage_custom) };
			AlertDialog.Builder builder = new AlertDialog.Builder(p);
			builder.setTitle(p.getResources().getString(R.string.storage_question));
			builder.setSingleChoiceItems(items, Globals.DownloadToSdcard ? 1 : 0, new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int item) 
				{
					dialog.dismiss();

					if( item == 2 )
						showCustomDownloadDirConfig(p);
					else
					{
						Globals.DownloadToSdcard = (item != 0);
						Globals.DataDir = Globals.DownloadToSdcard ?
										Settings.SdcardAppPath.get().bestPath(p) :
										p.getFilesDir().getAbsolutePath();
						goBack(p);
					}
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog)
				{
					goBack(p);
				}
			});
			AlertDialog alert = builder.create();
			alert.setOwnerActivity(p);
			alert.show();
		}
		static void showCustomDownloadDirConfig(final MainActivity p)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(p);
			builder.setTitle(p.getResources().getString(R.string.storage_custom));

			final EditText edit = new EditText(p);
			edit.setFocusableInTouchMode(true);
			edit.setFocusable(true);
			edit.setText(Globals.DataDir);
			builder.setView(edit);

			builder.setPositiveButton(p.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int item) 
				{
					Globals.DataDir = edit.getText().toString();
					dialog.dismiss();
					goBack(p);
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog)
				{
					goBack(p);
				}
			});
			AlertDialog alert = builder.create();
			alert.setOwnerActivity(p);
			alert.show();
		}
	}

	static class OptionalDownloadConfig extends Menu
	{
		boolean firstStart = false;
		OptionalDownloadConfig()
		{
			firstStart = true;
		}
		OptionalDownloadConfig(boolean firstStart)
		{
			this.firstStart = firstStart;
		}
		String title(final MainActivity p)
		{
			return p.getResources().getString(R.string.downloads);
		}
		void run (final MainActivity p)
		{
			String [] downloadFiles = Globals.DataDownloadUrl;
			
			AlertDialog.Builder builder = new AlertDialog.Builder(p);
			builder.setTitle(p.getResources().getString(R.string.downloads));

			final int itemsIdx[] = new int[downloadFiles.length];
			ArrayList<CharSequence> items = new ArrayList<CharSequence>();
			ArrayList<Boolean> enabledItems = new ArrayList<Boolean>();
			for(int i = 0; i < downloadFiles.length; i++ )
			{
				String item = new String(downloadFiles[i].split("[|]")[0]);
				boolean enabled = false;
				if( item.toString().indexOf("!") == 0 )
				{
					item = item.toString().substring(1);
					enabled = true;
				}
				if( item.toString().indexOf("!") == 0 ) // Download is mandatory
					continue;
				itemsIdx[items.size()] = i;
				items.add(item);
				enabledItems.add(enabled);
			}

			if( Globals.OptionalDataDownload == null || Globals.OptionalDataDownload.length != downloadFiles.length )
			{
				Globals.OptionalDataDownload = new boolean[downloadFiles.length];
				boolean oldFormat = true;
				for( int i = 0; i < downloadFiles.length; i++ )
				{
					if( downloadFiles[i].indexOf("!") == 0 )
					{
						Globals.OptionalDataDownload[i] = true;
						oldFormat = false;
					}
				}
				if( oldFormat )
					Globals.OptionalDataDownload[0] = true;
			}
			if( enabledItems.size() <= 0 )
			{
				goBack(p);
				return;
			}

			// Convert Boolean[] to boolean[], meh
			boolean[] enabledItems2 = new boolean[enabledItems.size()];
			for( int i = 0; i < enabledItems.size(); i++ )
				enabledItems2[i] = enabledItems.get(i);

			builder.setMultiChoiceItems(items.toArray(new CharSequence[0]), enabledItems2, new DialogInterface.OnMultiChoiceClickListener()
			{
				public void onClick(DialogInterface dialog, int item, boolean isChecked) 
				{
					Globals.OptionalDataDownload[itemsIdx[item]] = isChecked;
				}
			});
			builder.setPositiveButton(p.getResources().getString(R.string.ok), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item) 
				{
					dialog.dismiss();
					goBack(p);
				}
			});
			if( firstStart )
			{
				builder.setNegativeButton(p.getResources().getString(R.string.show_more_options), new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int item) 
					{
						dialog.dismiss();
						menuStack.clear();
						new MainMenu().run(p);
					}
				});
			}
			builder.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog)
				{
					goBack(p);
				}
			});
			AlertDialog alert = builder.create();
			alert.setOwnerActivity(p);
			alert.show();
		}
	}

	static class AudioConfig extends Menu
	{
		String title(final MainActivity p)
		{
			return p.getResources().getString(R.string.audiobuf_question);
		}
		void run (final MainActivity p)
		{
			final CharSequence[] items = {	p.getResources().getString(R.string.audiobuf_verysmall),
											p.getResources().getString(R.string.audiobuf_small),
											p.getResources().getString(R.string.audiobuf_medium),
											p.getResources().getString(R.string.audiobuf_large) };

			AlertDialog.Builder builder = new AlertDialog.Builder(p);
			builder.setTitle(R.string.audiobuf_question);
			builder.setSingleChoiceItems(items, Globals.AudioBufferConfig, new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int item) 
				{
					Globals.AudioBufferConfig = item;
					dialog.dismiss();
					goBack(p);
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog)
				{
					goBack(p);
				}
			});
			AlertDialog alert = builder.create();
			alert.setOwnerActivity(p);
			alert.show();
		}
	}

	static class VideoSettingsConfig extends Menu
	{
		static int debugMenuShowCount = 0;
		String title(final MainActivity p)
		{
			return p.getResources().getString(R.string.video);
		}
		//boolean enabled() { return true; };
		void run (final MainActivity p)
		{
			debugMenuShowCount++;
			CharSequence[] items = {
				p.getResources().getString(R.string.mouse_keepaspectratio),
				p.getResources().getString(R.string.video_smooth),
				p.getResources().getString(R.string.video_immersive),
				p.getResources().getString(R.string.video_draw_cutout),
				p.getResources().getString(R.string.video_orientation_autodetect),
				p.getResources().getString(R.string.video_orientation_vertical),
				p.getResources().getString(R.string.video_bpp_24),
				p.getResources().getString(R.string.tv_borders),
			};
			boolean defaults[] = {
				Globals.KeepAspectRatio,
				Globals.VideoLinearFilter,
				Globals.ImmersiveMode,
				Globals.DrawInDisplayCutout,
				Globals.AutoDetectOrientation,
				!Globals.HorizontalOrientation,
				Globals.VideoDepthBpp == 24,
				Globals.TvBorders,
			};

			if(Globals.SwVideoMode && !Globals.CompatibilityHacksVideo)
			{
				CharSequence[] items2 = {
					p.getResources().getString(R.string.mouse_keepaspectratio),
					p.getResources().getString(R.string.video_smooth),
					p.getResources().getString(R.string.video_immersive),
					p.getResources().getString(R.string.video_draw_cutout),
					p.getResources().getString(R.string.video_orientation_autodetect),
					p.getResources().getString(R.string.video_orientation_vertical),
					p.getResources().getString(R.string.video_bpp_24),
					p.getResources().getString(R.string.tv_borders),
					p.getResources().getString(R.string.video_separatethread),
				};
				boolean defaults2[] = { 
					Globals.KeepAspectRatio,
					Globals.VideoLinearFilter,
					Globals.ImmersiveMode,
					Globals.DrawInDisplayCutout,
					Globals.AutoDetectOrientation,
					!Globals.HorizontalOrientation,
					Globals.VideoDepthBpp == 24,
					Globals.TvBorders,
					Globals.MultiThreadedVideo,
				};
				items = items2;
				defaults = defaults2;
			}

			if(Globals.UsingSDL2)
			{
				CharSequence[] items2 = {
					p.getResources().getString(R.string.mouse_keepaspectratio),
				};
				boolean defaults2[] = { 
					Globals.KeepAspectRatio,
				};
				items = items2;
				defaults = defaults2;
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(p);
			builder.setTitle(p.getResources().getString(R.string.video));
			builder.setMultiChoiceItems(items, defaults, new DialogInterface.OnMultiChoiceClickListener() 
			{
				public void onClick(DialogInterface dialog, int item, boolean isChecked) 
				{
					if( item == 0 )
						Globals.KeepAspectRatio = isChecked;
					if( item == 1 )
						Globals.VideoLinearFilter = isChecked;
					if( item == 2 )
						Globals.ImmersiveMode = isChecked;
					if( item == 3 )
						Globals.DrawInDisplayCutout = isChecked;
					if( item == 4 )
						Globals.AutoDetectOrientation = isChecked;
					if( item == 5 )
						Globals.HorizontalOrientation = !isChecked;
					if( item == 6 )
						Globals.VideoDepthBpp = (isChecked ? 24 : 16);
					if( item == 7 )
						Globals.TvBorders = isChecked;
					if( item == 8 )
						Globals.MultiThreadedVideo = isChecked;
				}
			});
			builder.setPositiveButton(p.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int item) 
				{
					dialog.dismiss();
					goBack(p);
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog)
				{
					goBack(p);
				}
			});
			AlertDialog alert = builder.create();
			alert.setOwnerActivity(p);
			alert.show();
		}
	}

	static class ShowReadme extends Menu
	{
		String title(final MainActivity p)
		{
			return "Readme";
		}
		boolean enabled()
		{
			return true;
		}
		void run (final MainActivity p)
		{
			String readmes[] = Globals.ReadmeText.split("\\^");
			String lang = new String(Locale.getDefault().getLanguage()) + ":";
			if( p.isRunningOnOUYA() )
				lang = "tv:";
			String readme = readmes[0];
			String buttonName = "", buttonUrl = "";
			for( String r: readmes )
			{
				if( r.startsWith(lang) )
					readme = r.substring(lang.length());
				if( r.startsWith("button:") )
				{
					buttonName = r.substring("button:".length());
					if( buttonName.indexOf(":") != -1 )
					{
						buttonUrl = buttonName.substring(buttonName.indexOf(":") + 1);
						buttonName = buttonName.substring(0, buttonName.indexOf(":"));
					}
				}
			}
			readme = readme.trim();
			if( readme.length() <= 2 )
			{
				goBack(p);
				return;
			}
			TextView text = new TextView(p);
			text.setMaxLines(100);
			//text.setScroller(new Scroller(p));
			//text.setVerticalScrollBarEnabled(true);
			text.setText(readme);
			text.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			text.setPadding(0, 5, 0, 20);
			text.setTextSize(20.0f);
			text.setGravity(Gravity.CENTER);
			text.setFocusable(false);
			text.setFocusableInTouchMode(false);
			AlertDialog.Builder builder = new AlertDialog.Builder(p);
			ScrollView scroll = new ScrollView(p);
			scroll.setFocusable(false);
			scroll.setFocusableInTouchMode(false);
			scroll.addView(text, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			final Button ok = new Button(p);
			final AlertDialog alertDismiss[] = new AlertDialog[1];
			ok.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					alertDismiss[0].cancel();
				}
			});
			ok.setText(R.string.ok);
			LinearLayout layout = new LinearLayout(p);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.addView(scroll);
			layout.addView(ok);
			if( buttonName.length() > 0 )
			{
				Button cancel = new Button(p);
				cancel.setText(buttonName);
				final String url = buttonUrl;
				cancel.setOnClickListener(new View.OnClickListener()
				{
					public void onClick(View v)
					{
						if( url.length() > 0 )
						{
							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setData(Uri.parse(url));
							p.startActivity(i);
						}
						alertDismiss[0].cancel();
						System.exit(0);
					}
				});
				layout.addView(cancel);
			}
			builder.setView(layout);
			builder.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog)
				{
					goBack(p);
				}
			});
			AlertDialog alert = builder.create();
			alertDismiss[0] = alert;
			alert.setOwnerActivity(p);
			alert.show();
		}
	}

	static class GyroscopeCalibration extends Menu
	{
		String title(final MainActivity p)
		{
			return "";
		}
		boolean enabled()
		{
			return false;
		}
		void run (final MainActivity p)
		{
			goBack(p);
		}
	}

	public static class StorageAccessConfig extends Menu
	{
		public static int REQUEST_STORAGE_ID = 42;

		public static void onActivityResult(final MainActivity p, final int requestCode, final int resultCode, final Intent resultData)
		{
			if (requestCode == REQUEST_STORAGE_ID)
			{
				if (resultCode == Activity.RESULT_OK)
				{
					Uri treeUri = resultData.getData();
					p.getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
					Log.i("SDL", "Storage write permission granted to path " + treeUri.toString());
				}
				else
				{
					Log.i("SDL", "Storage write permission rejected");
				}
			}
		}

		String title(final MainActivity p)
		{
			return p.getResources().getString(R.string.storage_access);
		}
		void run (final MainActivity p)
		{
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
			{
				p.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_STORAGE_ID);
			}

			goBack(p);
		}
	}

	static class CommandlineConfig extends Menu
	{
		String title(final MainActivity p)
		{
			return p.getResources().getString(R.string.storage_commandline);
		}
		void run (final MainActivity p)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(p);
			builder.setTitle(p.getResources().getString(R.string.storage_commandline));

			final EditText edit = new EditText(p);
			edit.setFocusableInTouchMode(true);
			edit.setFocusable(true);
			if (Globals.CommandLine.length() == 0)
				Globals.CommandLine = "App";
			edit.setText(Globals.CommandLine.replace(" ", "\n").replace("	", " "));
			edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			edit.setMinLines(1);
			//edit.setMaxLines(100);
			builder.setView(edit);

			builder.setPositiveButton(p.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int item) 
				{
					Globals.CommandLine = "";
					String args[] = edit.getText().toString().split("\n");
					if( args.length == 1 )
					{
						Globals.CommandLine = args[0];
					}
					else
					{
						boolean firstArg = true;
						for( String arg: args )
						{
							if( !firstArg )
								Globals.CommandLine += " ";
							Globals.CommandLine += arg.replace(" ", "	");
							firstArg = false;
						}
					}
					dialog.dismiss();
					goBack(p);
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog)
				{
					goBack(p);
				}
			});
			AlertDialog alert = builder.create();
			alert.setOwnerActivity(p);
			alert.show();
		}
	}

	static class ResetToDefaultsConfig extends Menu
	{
		String title(final MainActivity p)
		{
			return p.getResources().getString(R.string.reset_config);
		}
		boolean enabled()
		{
			return true;
		}
		void run (final MainActivity p)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(p);
			builder.setTitle(p.getResources().getString(R.string.reset_config_ask));
			builder.setMessage(p.getResources().getString(R.string.reset_config_ask));
			
			builder.setPositiveButton(p.getResources().getString(R.string.ok), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item) 
				{
					Settings.DeleteSdlConfigOnUpgradeAndRestart(p); // Never returns
					dialog.dismiss();
					goBack(p);
				}
			});
			builder.setNegativeButton(p.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item) 
				{
					dialog.dismiss();
					goBack(p);
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog)
				{
					goBack(p);
				}
			});
			AlertDialog alert = builder.create();
			alert.setOwnerActivity(p);
			alert.show();
		}
	}
}
