var originalFormData;

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

function selectAllSectionsForWaitlists() {
  var checkAll = document.getElementById("selectAllSectionsForWaitlistsButton").checked;
  var inputList = document.querySelectorAll('li[id^="waitlist-section-"]:not(.hiddenElement) > input[id^="waitlist-section-"]');
  inputList.forEach(input => {
    input.checked = checkAll;
  });
}

function selectAllSectionsForTerm(element) {
  var checked = element.checked;
  var parentDiv = element.parentNode;
  var inputList = parentDiv.querySelectorAll('input');
  inputList.forEach(input => {
    input.checked = checked;
  });
}

function disableButtons() {
  // Anchors don't support the disabled attribute, so we need to add a disabled class
  var anchorList = document.querySelectorAll("a");
  anchorList.forEach(anchor => {
    anchor.classList.add("disabled");
    anchor.setAttribute("aria-disabled", "true");
  });
  var buttonList = document.querySelectorAll("button");
  buttonList.forEach(button => {
    button.disabled = true;
  });
}

function showLoadingIcon(element) {
  element.textContent=strings['button.loading'];
  const spinnerSpan = document.createElement('span');
  spinnerSpan.setAttribute("class", "spinner-border spinner-border-sm");
  spinnerSpan.setAttribute("role", "status");
  spinnerSpan.setAttribute("aria-hidden", "true");
  element.appendChild(spinnerSpan);
  document.getElementById("sr-text").textContent=strings['button.loading.srText'];
}

function hideAllTabs() {
    var tabList = document.querySelectorAll('[id^="tab-"]');
    tabList.forEach(tab => {
        hideElement(tab);
    });
}

function hideElement(element) {
    element.style.display = 'none';
    element.classList.add('hiddenElement');
}

function showElement(element) {
    element.style.display = '';
    element.classList.remove('hiddenElement');
}

function showAddRemoveSectionTab() {
    hideAllTabs();
    var tab = document.getElementById("tab-add-remove-sections");
    showElement(tab);
    document.getElementById("addRemoveSectionsHeader").focus();
}

function showWaitlistsTab(buttonId) {
    var inputList = document.querySelectorAll('input[id^="section-input"][data-waitlist-data-found="true"]:checked');
    // If no valid waitlists are selected/found then we skip that page, this way departments/schools (such as Wise) that
    // don't use waitlists can still use the section portions of the tool
    if(inputList.length === 0) {
        if(buttonId === "sections-next-button") {
            showValidateTab();
        } else {
            showAddRemoveSectionTab();
        }
        return;
    }

    var sections = [];
    inputList.forEach(input => {
        sections.push(input.value);
    });
    hideAllTabs();
    var potentialWaitlists = document.querySelectorAll('li[id^="waitlist-section-"]');
    potentialWaitlists.forEach(potentialWaitlist => {
        hideElement(potentialWaitlist);
        potentialWaitlist.disabled = true;
    });
    sections.forEach(section => {
        waitlistElement = document.getElementById("waitlist-section-" + section);
        showElement(waitlistElement);
        waitlistElement.disabled = false;
    });
    var tab = document.getElementById("tab-waitlists");
    showElement(tab);
    document.getElementById("waitlistsHeader").focus();
}

function showValidateTab() {
    var currentCourseSectionInputs = document.querySelectorAll('ul#currentCourseSections > li > input.form-check-input');
    var sectionsToAddInputs = document.querySelectorAll('ul[id^="sections-to-add-from-term-"] > li > input.form-check-input');
    var waitlistInputs = document.querySelectorAll('li[id^="waitlist-section-"]:not([style*="display: none"]) > input.form-check-input:not([disabled])');
    hideAllTabs();
    // Figure out what changes the user wants to make
    var sectionsToRemove = [];
    currentCourseSectionInputs.forEach(input => {
        if(input.checked !== input.defaultChecked) {
            sectionsToRemove.push(input);
        }
    });
    var sectionsToAdd = [];
    sectionsToAddInputs.forEach(input => {
        if(input.checked !== input.defaultChecked) {
            sectionsToAdd.push(input);
        }
    });
    var waitlistsToAdd = [];
    var waitlistsToRemove = [];
    waitlistInputs.forEach(input => {
        if(input.checked !== input.defaultChecked) {
            if(input.checked) {
                waitlistsToAdd.push(input);
            } else {
                waitlistsToRemove.push(input);
            }
        }
    });

    // Show the changes to make within the relevant HTML
    var saveButton = document.getElementById("save-button");
    var noChanges = document.getElementById("noChanges");
    hideElement(noChanges);
    var validateRemoveSections = document.getElementById("validateRemoveSections");
    hideElement(validateRemoveSections);
    var validateAddSections = document.getElementById("validateAddSections");
    hideElement(validateAddSections);
    var validateAddWaitlists = document.getElementById("validateAddWaitlists");
    hideElement(validateAddWaitlists);
    var validateRemoveWaitlists = document.getElementById("validateRemoveWaitlists");
    hideElement(validateRemoveWaitlists);
    if(sectionsToAdd.length === 0 && sectionsToRemove.length === 0 && waitlistsToAdd.length === 0 && waitlistsToRemove.length === 0) {
        saveButton.disabled = true;
        showElement(noChanges);
    } else {
        saveButton.disabled = false;
        if(sectionsToRemove.length > 0) {
            showElement(validateRemoveSections);
            var validateRemoveSectionsList = document.getElementById("validateRemoveSectionsList");
            validateRemoveSectionsList.innerHTML = "";
            var ul = document.createElement("ul");
            var sectionString = strings['validate.removeSections.section'];
            sectionsToRemove.forEach(section => {
                var li = document.createElement("li");
                li.innerHTML = stringInterpolation(sectionString, section.dataset.sectionSisId, section.dataset.totalStudents);
                ul.appendChild(li);
            });
            validateRemoveSectionsList.appendChild(ul);
        }
        if(sectionsToAdd.length > 0) {
            showElement(validateAddSections);
            var validateAddSectionsList = document.getElementById("validateAddSectionsList");
            validateAddSectionsList.innerHTML = "";
            var ul = document.createElement("ul");
            var sectionString = strings['validate.addSections.section'];
            sectionsToAdd.forEach(section => {
                var li = document.createElement("li");
                li.innerHTML = stringInterpolation(sectionString, section.dataset.sectionSisId, section.dataset.totalStudents);
                ul.appendChild(li);
            });
            validateAddSectionsList.appendChild(ul);
        }
        if(waitlistsToAdd.length > 0) {
            showElement(validateAddWaitlists);
            var validateAddWaitlistsList = document.getElementById("validateAddWaitlistsList");
            validateAddWaitlistsList.innerHTML = "";
            var ul = document.createElement("ul");
            var sectionString = strings['validate.addWaitlists.section'];
            waitlistsToAdd.forEach(section => {
                var li = document.createElement("li");
                li.innerHTML = stringInterpolation(sectionString, section.dataset.sectionSisId);
                ul.appendChild(li);
            });
            validateAddWaitlistsList.appendChild(ul);
        }
        if(waitlistsToRemove.length > 0) {
            showElement(validateRemoveWaitlists);
            var validateRemoveWaitlistsList = document.getElementById("validateRemoveWaitlistsList");
            validateRemoveWaitlistsList.innerHTML = "";
            var ul = document.createElement("ul");
            var sectionString = strings['validate.removeWaitlists.section'];
            waitlistsToRemove.forEach(section => {
                var li = document.createElement("li");
                li.innerHTML = stringInterpolation(sectionString, section.dataset.sectionSisId);;
                ul.appendChild(li);
            });
            validateRemoveWaitlistsList.appendChild(ul);
        }
    }
    var tab = document.getElementById("tab-validate");
    showElement(tab);
    document.getElementById("validateHeader").focus();
}

function termCheck(buttonId) {
  var courseTermName = document.getElementById("course-term-name").value;
  var rostersToCrosslist = document.querySelectorAll("input:checked[data-term]");
  var differentTerms = new Set();
  rostersToCrosslist.forEach(function(rosterElement) {
    var term = rosterElement.dataset.term;
    if (term !== courseTermName) {
      differentTerms.add(term);
    }
  });
  if(differentTerms.size === 0) {
    showWaitlistsTab(buttonId);
  } else {
    var text = strings['alert.multipleTerms.text'];
    message = stringInterpolation(text, differentTerms.values().next().value, courseTermName);
    Swal.fire({
      title: strings['alert.multipleTerms.title'],
      html: message,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: strings['alert.multipleTerms.confirmText']
    }).then((result) => {
      if(result.isConfirmed) {
        showWaitlistsTab(buttonId);
      }
    });
  }
}

// While not perfect, this is an attempt to replicate the string interpolation that thymeleaf uses so we don't
// have to worry about different message formats depending on whether the message came from Java or Javascript
function stringInterpolation(message, ...vars) {
  for(var i=0; i<vars.length; i++) {
    message = message.replace(`{${i}}`, vars[i]);
  }
  return message;
}

document.addEventListener("DOMContentLoaded", () => {
  var form = document.querySelector("form");
  form.addEventListener("submit", (event) => {
    showLoadingIcon(event.submitter);
    disableButtons();
  });
});