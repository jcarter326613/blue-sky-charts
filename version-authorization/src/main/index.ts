import { EasyAwait, LambdaEntry } from 'easy-await'

export const appVersion = async (event: any = {}): Promise<any> => {
    let entry = new LambdaEntry();
    let existingVersionApp: string | undefined
    let existingVersionPrivacy: string | undefined
    let appUpdateNeeded: boolean = false
    let policyUpdateNeeded: boolean = false

    return entry.handleGetRequest(event, true, (parameters) => {
        if (!("appVersion" in parameters)) {
            EasyAwait.instance.reportUserError("Missing or invalid field appVersion");
            return;
        } else if (!("privacyVersion" in parameters)) {
            EasyAwait.instance.reportUserError("Missing or invalid field privacyVersion");
            return;
        }

        //Pull out the arguments
        existingVersionApp = parameters["appVersion"]
        existingVersionPrivacy = parameters["privacyVersion"]

        //Fulfill the request
        appUpdateNeeded = false
        policyUpdateNeeded = true
    }, () => {
        let results: any = {
            appUpdateNeeded: appUpdateNeeded,
            policyUpdateNeeded: policyUpdateNeeded
        }
        return JSON.stringify(results);
    });
}
