FROM ubuntu:14.04

ADD . /
ENV GOOGLE_APPLICATION_CREDENTIALS /service-account.json
ENV PYTHONUNBUFFERED 1

RUN apt-get update && apt-get install -y python-pip python2.7 python2.7-dev python3.4
RUN pip install -r requirements.txt

CMD python /write.py
