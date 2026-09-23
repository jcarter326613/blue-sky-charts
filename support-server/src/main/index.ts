
import { EasyAwait, LambdaEntry } from 'easy-await'
import { MailClient } from './mail-client'

export const receiveWebsiteMessage = async (event: any = {}): Promise<any> => {
    let entry = new LambdaEntry();
    return entry.handlePostRequest(event, true, (parameters, body) => {
        //Validate Paraemeters
        if (!("name" in parameters)) {
            EasyAwait.instance.reportUserError("Missing or invalid field name");
            return;
        }
        let fromName = parameters["name"] as string;
        fromName = fromName.trim();
        if ( fromName.length == 0 ) {
            EasyAwait.instance.reportUserError("Name can not be blank");
            return;
        }
        if (!("email" in parameters)) {
            EasyAwait.instance.reportUserError("Missing or invalid field email");
            return;
        }
        let fromEmail = parameters["email"] as string;
        fromEmail = fromEmail.trim();
        if ( fromEmail.length == 0 ) {
            EasyAwait.instance.reportUserError("Email can not be blank");
            return;
        }
        if ( body === undefined || body.length == 0 ) {
            EasyAwait.instance.reportUserError("Message can not be blank");
            return;
        }

        //Fulfill the request
        let mailClient = new MailClient();
        mailClient.receiveMail(fromName, fromEmail, body);
    }, () => {
        return "";
    });
}
