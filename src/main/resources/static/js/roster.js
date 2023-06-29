function selectRoster() {
  var submitButton = document.getElementById("formSubmitButton");
  submitButton.disabled = false;
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

function showWaitlists(source) {
  var waitlists = document.querySelectorAll("li[data-waitlist='true']");
  var showWaitlists = source.checked;
  if(showWaitlists) {
    waitlists.forEach(waitlist => {
      waitlist.style.display = 'block';
      waitlist.style.visibility = 'visible';
    });
  } else {
    waitlists.forEach(waitlist => {
      waitlist.style.display = 'none';
      waitlist.style.visibility = 'hidden';
    });
  }
}