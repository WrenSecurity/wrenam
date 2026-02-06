navigator.credentials.create({
    publicKey: PublicKeyCredential.parseCreationOptionsFromJSON({publicKey})
}).then(credential => {
    document.getElementById("clientScriptOutputData").value = JSON.stringify(credential);
    document.getElementById('button_1').click();
}).catch(function (error) {
    document.getElementById('clientScriptOutputData').value = error.message;
    document.getElementById('button_2').click();
});
