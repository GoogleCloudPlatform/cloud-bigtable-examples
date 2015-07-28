package connector;
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.http.HttpBackOffIOExceptionHandler
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpResponse
import com.google.api.client.http.HttpUnsuccessfulResponseHandler
import com.google.api.client.util.ExponentialBackOff
import com.google.api.client.util.Sleeper
import com.google.common.base.Preconditions
import java.io.IOException
import java.util.logging.Logger

/**
  *  THIS WAS TAKEN FROM THE CMDLINE-PULL EXAMPLE OF GOOGLE CLOUD PLATFORM and translated into scala: 
  *  https://github.com/GoogleCloudPlatform/cloud-pubsub-samples-java/tree/master/cmdline-pull
  * 
  *  RetryHttpInitializerWrapper will automatically retry upon RPC
  *  failures, preserving the auto-refresh behavior of the Google
  *  Credentials.
  */
class RetryHttpInitializerWrapper(_wrappedCredential: Credential) extends HttpRequestInitializer {

  /**
    *  A private logger.
    */
  private val LOG =
    Logger.getLogger("RetryHttpInitializerWrapper".getClass.getName)

  /**
    *  One minutes in miliseconds.
    */
  private val ONEMINITUES = 60000

  /**
    *  Intercepts the request for filling in the "Authorization"
    *  header field, as well as recovering from certain unsuccessful
    *  error codes wherein the Credential must refresh its token for a
    *  retry.
    */
  private var wrappedCredential: Credential = _wrappedCredential

  /**
    *  A sleeper; you can replace it with a mock in your test.
    */
  private var sleeper: Sleeper = Sleeper.DEFAULT

  override def initialize(request: HttpRequest) {
    request.setReadTimeout(2 * ONEMINITUES); // 2 minutes read timeout
    val backoffHandler =
      new HttpBackOffUnsuccessfulResponseHandler(
        new ExponentialBackOff())
        .setSleeper(sleeper)
    request.setInterceptor(wrappedCredential)
    request.setUnsuccessfulResponseHandler(
      new HttpUnsuccessfulResponseHandler() {
        override def handleResponse(
          request: HttpRequest,
          response: HttpResponse,
          supportsRetry: Boolean) : Boolean = {
          if (wrappedCredential.handleResponse(
            request, response, supportsRetry)) {
            // If credential decides it can handle it,
            // the return code or message indicated
            // something specific to authentication,
            // and no backoff is desired.
            true
          } else if (backoffHandler.handleResponse(
            request, response, supportsRetry)) {
            // Otherwise, we defer to the judgement of
            // our internal backoff handler.
            LOG.info("Retrying "+ request.getUrl().toString())
            true
          } else {
            false
          }
        }
      });
    request.setIOExceptionHandler(
      new HttpBackOffIOExceptionHandler(new ExponentialBackOff())
        .setSleeper(sleeper));
  }
}
