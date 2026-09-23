import * as AWS from 'aws-sdk';
import { EasyAwait } from 'easy-await';

export class MailClient {
    private static readonly SUPPORT_EMAIL = "muddypawcloudtechnology@gmail.com" 
    private sesClient: AWS.SES;

    constructor() {
        AWS.config.region = "us-east-1";
        this.sesClient = new AWS.SES({apiVersion: '2010-12-01'});
    }

    public receiveMail(fromName: string, fromEmail: string, message: string) {
        EasyAwait.instance.startThread("MailClient.receiveMail");
        this.sesClient.sendEmail( {
            "Destination": {
                "ToAddresses": [MailClient.SUPPORT_EMAIL]
            },
            "Source": MailClient.SUPPORT_EMAIL,
            "ReplyToAddresses": [`${fromName}<${fromEmail}>`],
            "Message": {
                "Subject": {
                    "Data": "Message from BlueSkyCharts.com"
                },
                "Body": {
                    "Text": {
                        "Data": message
                    }
                }
            }
        }, (err: AWS.AWSError, data: AWS.SES.SendEmailResponse): void => {
            if (err) {
                EasyAwait.instance.reportFatalError(`Error sending email from ${fromName}<${fromEmail}>.  Body: ${message}.`);
            }
            EasyAwait.instance.endThread("MailClient.receiveMail");
        });
    }
}