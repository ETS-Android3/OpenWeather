package fr.qgdev.openweather.dialog;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import fr.qgdev.openweather.Place;
import fr.qgdev.openweather.R;
import fr.qgdev.openweather.WeatherService;
import fr.qgdev.openweather.dataplaces.DataPlaces;
import fr.qgdev.openweather.dataplaces.PlaceAlreadyExistException;
import fr.qgdev.openweather.ui.places.PlacesFragment;

public class AddPlaceDialog extends Dialog {

	private final Context context;
	private final ConstraintLayout dialogWindow;
	private final TextInputLayout cityTextInputLayout, countryTextInputLayout;
	private final TextInputEditText cityEditText;
	private final AutoCompleteTextView countryEditText;
	private final ProgressBar addButtonProgressSpinner;
	private final Button exitButton, addButton;

	private final WeatherService.WeatherCallbackGetCoordinates getCoordinatesCallback;
	private final WeatherService.WeatherCallbackGetData getDataCallback;

	private final List<String> countryNames;
	private final List<String> countryNamesSorted;
	private final List<String> countryCodes;

	public AddPlaceDialog(Context context, View addPlaceFABView, String apiKey, WeatherService weatherService, PlacesFragment.Interactions placeFragmentInteractions) {
		super(context);
		setContentView(R.layout.dialog_add_place);

		this.context = context;

		this.dialogWindow = findViewById(R.id.dialog_window);
		this.cityTextInputLayout = findViewById(R.id.cityTextInputLayout);
		this.countryTextInputLayout = findViewById(R.id.countryTextInputLayout);
		this.cityEditText = findViewById(R.id.city);
		this.countryEditText = findViewById(R.id.country);
		this.exitButton = findViewById(R.id.exit_button);
		this.addButton = findViewById(R.id.add_button);

		this.addButtonProgressSpinner = findViewById(R.id.add_button_progress_spinner);
		addButtonProgressSpinner.setVisibility(View.GONE);

		this.countryNames = Arrays.asList(context.getResources().getStringArray(R.array.countries_names));
		this.countryNamesSorted = Arrays.asList(context.getResources().getStringArray(R.array.countries_names));
		Collections.sort(this.countryNamesSorted);
		this.countryCodes = Arrays.asList(context.getResources().getStringArray(R.array.countries_codes));

		ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.dialog_country_list_item, countryNamesSorted);
		countryEditText.setThreshold(1);
		countryEditText.performValidation();
		countryEditText.setAdapter(adapter);

		//  Observe country field to show an error if the registered country name doesn't exist
		countryEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				//  The country name doesn't exist, the field is not focused and the field isn't empty
				if (getCountryCode() == null && !hasFocus && !getCountryField().isEmpty()) {
					countryTextInputLayout.setError(context.getString(R.string.error_place_country_not_in_list));
				} else {
					countryTextInputLayout.setErrorEnabled(false);
				}
			}
		});

		//  Observe city field to turn off any errors
		cityEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				cityTextInputLayout.setErrorEnabled(false);
			}
		});


		//  WeatherService Callbacks
		//________________________________________________________________
		//  This callback will fill place with data
		getCoordinatesCallback = new WeatherService.WeatherCallbackGetCoordinates() {
			@Override
			public void onPlaceFound(Place place, DataPlaces dataPlaces) {
				weatherService.getWeatherDataOWM(place, dataPlaces, getDataCallback);
			}

			@Override
			public void onTreatmentError() {
				Snackbar.make(dialogWindow, context.getString(R.string.error_cannot_save_place_treatment), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
						.show();
				enableDialogWindowControls();

			}

			@Override
			public void onNoResponseError() {
				Snackbar.make(dialogWindow, context.getString(R.string.error_server_unreachable), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
						.show();
				enableDialogWindowControls();
			}

			@Override
			public void onTooManyRequestsError() {
				Snackbar.make(dialogWindow, context.getString(R.string.error_too_many_request_in_a_day), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
						.show();
				enableDialogWindowControls();
			}

			@Override
			public void onPlaceNotFoundError() {
				cityTextInputLayout.setError(context.getString(R.string.error_place_not_found));
				enableDialogWindowControls();
			}

			@Override
			public void onWrongOrUnknownApiKeyError() {
				Snackbar.make(addPlaceFABView, context.getString(R.string.error_wrong_api_key), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
						.show();
				enableDialogWindowControls();

			}

			@Override
			public void onUnknownError() {
				Snackbar.make(addPlaceFABView, context.getString(R.string.error_unknow_error), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
						.show();
				enableDialogWindowControls();

			}

			@Override
			public void onDeviceNotConnected() {
				Snackbar.make(dialogWindow, context.getString(R.string.error_device_not_connected), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
						.show();
				enableDialogWindowControls();

			}

			@Override
			public void onTheEndOfTheRequest() {
			}
		};

		//________________________________________________________________
		//  This callback will fill place with data
		this.getDataCallback = new WeatherService.WeatherCallbackGetData() {
			@Override
			public void onWeatherData(final Place place, DataPlaces dataPlaces) {
				try {
					if (dataPlaces.addPlace(place)) {
						placeFragmentInteractions.onAddingPlace(dataPlaces, dataPlaces.getPlacePositionInRegister(place), place);
						dismiss();
					} else {
						throw new Exception("Commit Error");
					}
				} catch (PlaceAlreadyExistException e) {
					e.printStackTrace();
					cityTextInputLayout.setError(context.getString(R.string.error_place_already_added));
				} catch (Exception e) {
					e.printStackTrace();
					Snackbar.make(dialogWindow, context.getString(R.string.error_cannot_save_place), Snackbar.LENGTH_SHORT)
							.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
							.show();
				}
			}

			@Override
			public void onTreatmentError() {
				Snackbar.make(dialogWindow, context.getString(R.string.error_cannot_save_place_treatment), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
						.show();
			}

			@Override
			public void onNoResponseError() {
				Snackbar.make(dialogWindow, context.getString(R.string.error_server_unreachable), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
						.show();
			}

			@Override
			public void onTooManyRequestsError() {
				Snackbar.make(dialogWindow, context.getString(R.string.error_too_many_request_in_a_day), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
						.show();
			}

			@Override
			public void onPlaceNotFoundError() {
				Snackbar.make(dialogWindow, context.getString(R.string.error_place_not_found), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
						.show();
			}

			@Override
			public void onWrongOrUnknownApiKeyError() {
				Snackbar.make(dialogWindow, context.getString(R.string.error_wrong_api_key), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
						.show();
			}

			@Override
			public void onUnknownError() {
				Snackbar.make(dialogWindow, context.getString(R.string.error_unknow_error), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
						.show();
			}

			@Override
			public void onDeviceNotConnected() {
				Snackbar.make(dialogWindow, context.getString(R.string.error_device_not_connected), Snackbar.LENGTH_SHORT)
						.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
						.show();
			}

			@Override
			public void onTheEndOfTheRequest() {
				enableDialogWindowControls();
			}
		};


		//  Verify button click listener
		addButton.setOnClickListener(
				verifyButtonView -> {
					disableDialogWindowControls();

					//  Nothing was registered
					if (getCityField().isEmpty() || getCountryField().isEmpty() || getCountryCode() == null) {
						if (getCityField().isEmpty())
							cityTextInputLayout.setError(context.getString(R.string.error_place_city_field_empty));
						if (getCountryField().isEmpty())
							countryTextInputLayout.setError(context.getString(R.string.error_place_country_field_empty));
						else if (getCountryCode() == null)
							countryTextInputLayout.setError(context.getString(R.string.error_place_country_not_in_list));

						enableDialogWindowControls();
					}

					//  No API key is registered
					else if (apiKey == null || apiKey.length() != 32) {
						dismiss();
						Snackbar.make(dialogWindow, context.getString(R.string.error_no_api_key_registered), Snackbar.LENGTH_SHORT)
								.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setMaxInlineActionWidth(3)
								.show();
						enableDialogWindowControls();

					}

					//  API key and place settings is correctly registered
					else {
						weatherService.getCoordinatesOWM(new Place(getCityField(), getCountryCode()), getCoordinatesCallback);
					}
				});

		exitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
	}


	public String getCityField() {
		Editable city = cityEditText.getText();
		if (city.length() == 0) {
			return "";
		}
		return city.toString();
	}

	public String getCountryCode() {
		int indexOf = countryNames.indexOf(getCountryField());

		if (indexOf == -1) {
			return null;
		}
		return countryCodes.get(indexOf);
	}

	public String getCountryField() {
		Editable country = countryEditText.getText();
		if (country.length() == 0) {
			return "";
		}
		return country.toString();
	}

	public void disableDialogWindowControls() {
		addButton.setEnabled(false);
		exitButton.setEnabled(false);
		addButtonProgressSpinner.setVisibility(View.VISIBLE);
	}

	public void enableDialogWindowControls() {
		addButton.setEnabled(true);
		exitButton.setEnabled(true);
		addButtonProgressSpinner.setVisibility(View.GONE);
	}

	public void build() {
		show();
	}
}
