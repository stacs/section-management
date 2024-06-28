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
  var inputList = document.querySelectorAll('[id^="waitlist-section-"]');
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
    element.style.visibility = 'hidden';
}

function showElement(element) {
    element.style.display = '';
    element.style.visibility = 'visible';
}

function showAddRemoveSectionTab() {
    hideAllTabs();
    var tab = document.getElementById("tab-add-remove-sections");
    showElement(tab);
    document.getElementById("addRemoveSectionsHeader").focus();
}

function showWaitlistsTab() {
    var inputList = document.querySelectorAll('[id^="section-input"]:checked');
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
    hideAllTabs();
    // Figure out what changes the user wants to make
    var sectionsToRemove = [];
    var currentCourseSectionInputs = document.querySelectorAll('ul#currentCourseSections > li > input.form-check-input');
    currentCourseSectionInputs.forEach(input => {
        if(input.checked !== input.defaultChecked) {
            sectionsToRemove.push(input.dataset.sectionSisId);
        }
    });
    var sectionsToAdd = [];
    var sectionsToAddInputs = document.querySelectorAll('ul[id^="sections-to-add-from-term-"] > li > input.form-check-input');
    sectionsToAddInputs.forEach(input => {
        if(input.checked !== input.defaultChecked) {
            sectionsToAdd.push(input.dataset.sectionSisId);
        }
    });
    var waitlistsToAdd = [];
    var waitlistsToRemove = [];
    var waitlistInputs = document.querySelectorAll('li[id^="waitlist-section-"] > input.form-check-input:not([disabled])');
    waitlistInputs.forEach(input => {
        if(input.checked !== input.defaultChecked) {
            if(input.checked) {
                waitlistsToAdd.push(input.dataset.sectionSisId);
            } else {
                waitlistsToRemove.push(input.dataset.sectionSisId);
            }
        }
    });

    // Show the changes to make within the relevant HTML
    if(sectionsToRemove.length > 0) {
        var validateRemoveSections = document.getElementById("validateRemoveSections");
        validateRemoveSections.innerHTML = "";
        var ul = document.createElement("ul");
        sectionsToRemove.forEach(section => {
            var li = document.createElement("li");
            li.innerHTML = section;
            ul.appendChild(li);
        });
        validateRemoveSections.appendChild(ul);
    }
    if(sectionsToAdd.length > 0) {
        var validateAddSections = document.getElementById("validateAddSections");
        validateAddSections.innerHTML = "";
        var ul = document.createElement("ul");
        sectionsToAdd.forEach(section => {
            var li = document.createElement("li");
            li.innerHTML = section;
            ul.appendChild(li);
        });
        validateAddSections.appendChild(ul);
    }
    if(waitlistsToAdd.length > 0) {
        var validateAddWaitlists = document.getElementById("validateAddWaitlists");
        validateAddWaitlists.innerHTML = "";
        var ul = document.createElement("ul");
        waitlistsToAdd.forEach(section => {
            var li = document.createElement("li");
            li.innerHTML = section;
            ul.appendChild(li);
        });
        validateAddWaitlists.appendChild(ul);
    }
    if(waitlistsToRemove.length > 0) {
        var validateRemoveWaitlists = document.getElementById("validateRemoveWaitlists");
        validateRemoveWaitlists.innerHTML = "";
        var ul = document.createElement("ul");
        waitlistsToRemove.forEach(section => {
            var li = document.createElement("li");
            li.innerHTML = section;
            ul.appendChild(li);
        });
        validateRemoveWaitlists.appendChild(ul);
    }
    var tab = document.getElementById("tab-validate");
    showElement(tab);
    document.getElementById("validateHeader").focus();
}

function termCheck() {
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
    showWaitlistsTab();
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
        showWaitlistsTab();
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