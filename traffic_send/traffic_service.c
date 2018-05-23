/*
 * multi_thread_socket_server.c
 *
 *  Created on: Mar 14, 2014
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
#define BUFFER_LENGTH 65536
#define MAX_CONN_LIMIT 512  //MAX connection limit

static void Data_handle(void * sock_fd);   //Only can be seen in the file

int main()
{
    int sockfd_server;
    //int sockfd;
    int fd_temp;
    struct sockaddr_in s_addr_in;
    //int client_length;

    sockfd_server = socket(AF_INET,SOCK_STREAM,0);  //ipv4,TCP
    assert(sockfd_server != -1);

    //before bind(), set the attr of structure sockaddr.
    memset(&s_addr_in,0,sizeof(s_addr_in));
    s_addr_in.sin_family = AF_INET;
    s_addr_in.sin_addr.s_addr = htonl(INADDR_ANY);  //trans addr from uint32_t host byte order to network byte order.
    s_addr_in.sin_port = htons(SOCK_PORT);          //trans port from uint16_t host byte order to network byte order. 
    int nZero = 0;
    setsockopt(sockfd_server,SOL_SOCKET,SO_RCVBUF,(char *)&nZero,sizeof(int));
    fd_temp = bind(sockfd_server,(struct scokaddr *)(&s_addr_in),sizeof(s_addr_in));
    if(fd_temp == -1)
    {
        fprintf(stderr,"bind error!\n");
        exit(1);
    }

    fd_temp = listen(sockfd_server,MAX_CONN_LIMIT);
    if(fd_temp == -1)
    {
        fprintf(stderr,"listen error!\n");
        exit(1);
    }

    while(1)
    {
        printf("waiting for new connection...\n");
        pthread_t *thread_id = (pthread_t *)malloc(sizeof(pthread_t));
        
        int *client_length = (int*)malloc(sizeof(int)); 
  	*client_length = sizeof(struct sockaddr_in);

        struct sockaddr_in *s_addr_client = (struct sockaddr_in*) malloc(sizeof(struct sockaddr_in));

        //Block here. Until server accpets a new connection.
        int *sockfd = (int *)malloc(sizeof(int));
        *sockfd = accept(sockfd_server,(struct sockaddr_*)(s_addr_client),(socklen_t *)(client_length));
        if(sockfd == -1)
        {
            fprintf(stderr,"Accept error!\n");
            continue;                               //ignore current socket ,continue while loop.
        }
        printf("A new connection occurs!\n");
        if(pthread_create(thread_id,NULL,(void *)(&Data_handle),(void *)(sockfd)) == -1)
        {
            fprintf(stderr,"pthread_create error!\n");
            break;                                  //break while loop
        }
    }

    //Clear
    int ret = shutdown(sockfd_server,SHUT_WR); //shut down the all or part of a full-duplex connection.
    assert(ret != -1);

    printf("Server shuts down\n");
    return 0;
}

static void Data_handle(void * sock_fd)
{
    int fd = *((int *)sock_fd);
    //int nZero = 0;
    //setsockopt(fd,SOL_SOCKET,SO_RCVBUF,(char *)&nZero,sizeof(int));
    //printf("socket fd: %d \t mem address: %d\n",fd,sock_fd);
    int i_recvBytes;
    char *data_recv = (char *)malloc(sizeof(char)*BUFFER_LENGTH);
    //const char * data_send = "Server has received your request!\n";
    //printf("buff: address: %d\n",data_recv);

    while(1)
    {
        //printf("waiting for request...\n");
        //Reset data.
        //memset(data_recv,0,BUFFER_LENGTH);

        i_recvBytes = read(fd,data_recv,BUFFER_LENGTH-1);
        //printf("buff: address: %d\n",data_recv);
        if(i_recvBytes == 0)
        {
            printf("Maybe the client has closed\n");
            break;
        }
        if(i_recvBytes == -1)
        {
            fprintf(stderr,"read error!\n");
            break;
        }
        if(strcmp(data_recv,"quit")==0)
        {
            printf("Quit command!\n");
            break;                           //Break the while loop.
        }
        printf("read from client : %s\n",data_recv);
     //   if(write(fd,data_send,strlen(data_send)) == -1)
      //  {
       //     break;
        //}
    }

    //Clear
    free(data_recv);
    printf("terminating current client_connection...\n");
    close(fd);            //close a file descriptor.
    pthread_exit(NULL);   //terminate calling thread!
}
