/*
 * socket_client.c
 *
 *  Created on: Mar 15, 2014
 *      Author: nerohwang
 */
#include<stdlib.h>
#include<pthread.h>
#include<sys/socket.h>
#include<sys/types.h>       //pthread_t , pthread_attr_t and so on.
#include<stdio.h>
#include<netinet/in.h>      //structure sockaddr_in
#include<arpa/inet.h>       //Func : htonl; htons; ntohl; ntohs
#include<assert.h>          //Func :assert
#include<string.h>          //Func :memset
#include<unistd.h>          //Func :close,write,read
#define SOCK_PORT 60000
#define BUFFER_LENGTH 40

typedef struct socketInfo{
  int tid;
  char *targetIp;
  int  delaySendTime; // seconds
  int  sendPort;
  double speed; // e.g: 0.2 seconds/packet
  char *selfIp;
}Info;

char *sendData;

static void sendClient(void *args);

/*main function*/
int main(int narg , char *args[]){
   sendData = (char *)malloc(sizeof(char) * BUFFER_LENGTH);
   memset(sendData,0,BUFFER_LENGTH);

   for(int i = 0 ;i < BUFFER_LENGTH - 1; ++i){
     sendData[i] = '1';
   }
   
   char *ip = "127.0.0.1";
   int flowNum = 40;
  
  
  pthread_t *tid = (pthread_t*)malloc(sizeof(pthread_t) * flowNum);;

  for(int i = 0 ; i < flowNum; i++){

    Info *info = (Info *)malloc(sizeof(Info));
    info->tid = i;
    info->targetIp = ip;
    info->delaySendTime = (rand()%60)+1;
    info->sendPort = 50000+i;

    if(pthread_create(&tid[i],NULL,(void*)(&sendClient),(void *) (info)) == -1){
         fprintf(stderr,"pthread_create error!\n");
    }
    
  }

  for(int i = 0 ;i < flowNum; ++i){
     pthread_join(tid[i],NULL);
  }
  return 0;
}

void sendClient(void *args)
{
    Info *info = (Info *)args;
    
    fprintf(stderr,"%d thread has start up ! \t delay time: %d\n",info->tid,info->delaySendTime);
    char fileDir[40];
    sprintf(fileDir,"%s-%d",info->selfIp,info->tid);

    File *f = open(fileDir,'w');

    sleep(info->delaySendTime); // thread sleep
    int sockfd;
    int tempfd;
    struct sockaddr_in s_addr_in;
    struct sockaddr_in client;
    char data_send[BUFFER_LENGTH];
    memset(data_send,0,BUFFER_LENGTH);

    sockfd = socket(AF_INET,SOCK_STREAM,0);       //ipv4,TCP
    if(sockfd == -1)
    {
        fprintf(stderr,"socket error!\n");
        exit(1);
    }

    //before func connect, set the attr of structure sockaddr.
    memset(&s_addr_in,0,sizeof(s_addr_in));
    s_addr_in.sin_addr.s_addr = inet_addr(info->targetIp);      //trans char * to in_addr_t
    s_addr_in.sin_family = AF_INET;
    s_addr_in.sin_port = htons(SOCK_PORT);

    client.sin_family = AF_INET;
    client.sin_addr.s_addr = htonl(INADDR_ANY);
    client.sin_port = htons(info->sendPort);

    int nZero = 0;
    setsockopt(sockfd,SOL_SOCKET,SO_SNDBUF,(char *)&nZero,sizeof(int));

    if (bind( sockfd, (struct sockaddr*) &client, sizeof(client)) == -1) {
       printf("bind() failed.\n");
       shutdown(sockfd,SHUT_WR);
       return 1;
    }

    tempfd = connect(sockfd,(struct sockaddr *)(&s_addr_in),sizeof(s_addr_in));
    if(tempfd == -1)
    {
        fprintf(stderr,"Connect error! \n");
        exit(1);
    }

   int cnt = 20;

    while((cnt--) > 0)
    {
        fprintf(stderr,"%d thread send data!\n",info->tid);
        //gets(data_send);
        //scanf("%[^\n]",data_send);         //or you can also use this
        tempfd = write(sockfd,sendData,strlen(sendData));
        if(tempfd == -1)
        {
            fprintf(stderr,"write error\n");
            exit(0);
        }

        if(strcmp(data_send,"quit") == 0)  //quit,write the quit request and shutdown client
        {
            break;
        }
        else
        {
            /*tempfd = read(sockfd,data_recv,BUFFER_LENGTH);
            assert(tempfd != -1);
            printf("%s\n",data_recv);
            memset(data_send,0,BUFFER_LENGTH);
            memset(data_recv,0,BUFFER_LENGTH);
            */
        }
	file.write("cheng %d\n",cnt);
        usleep(1000*1000);
        //select(0,NULL,NULL,NULL,&delay);
    }

    file.flush();
    file.close();

    fprintf(stderr,"%d exit !\n",info->tid);
    int ret = shutdown(sockfd,SHUT_WR);       //or you can use func close()--<unistd.h> to close the fd
    assert(ret != -1);
    return 0;
}
