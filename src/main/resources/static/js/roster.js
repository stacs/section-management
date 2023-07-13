var originalFormData;

function selectRoster() {
  var submitButton = document.getElementById("formSubmitButton");
  var newFormData = getFormData();
  if(JSON.stringify(originalFormData) == JSON.stringify(newFormData)) {
    submitButton.disabled = true;
  } else {
    submitButton.disabled = false;
  }
}

function selectTerm() {
  var divList = document.querySelectorAll('[id^="term-div-"]');
  divList.forEach(div => {
    div.style.display = 'none';
    div.style.visibility = 'hidden';
  });
  var selectElement = document.getElementById("term-select");
  var selectedTerm = selectElement.value;
  // -1 is the value for the 'Select Term' option
  if(selectedTerm == -1) {
    return;
  }
  var selectedTermDiv = document.getElementById('term-div-' + selectedTerm);
  selectedTermDiv.style.display = '';
  selectedTermDiv.style.visibility = 'visible';
}

// Used to determine when the Continue button for the initial form should be enabled/disabled.
function getFormData() {
  return $('input:checked').map(function() {
    return this.id;
  }).get();
}

$(document).ready(function() {
  originalFormData = getFormData();
});