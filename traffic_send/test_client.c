/*
 * socket_client.c
 *
 *  Created on: Mar 15, 2014
 *      Author: nerohwang
 */
#include<stdlib.h>
#include<sys/socket.h>
#include<sys/types.h>       //pthread_t , pthread_attr_t and so on.
#include<stdio.h>
#include<netinet/in.h>      //structure sockaddr_in
#include<arpa/inet.h>       //Func : htonl; htons; ntohl; ntohs
#include<assert.h>          //Func :assert
#include<string.h>          //Func :memset
#include<unistd.h>          //Func :close,write,read
#define SOCK_PORT 60000
#define BUFFER_LENGTH 1024
int main()
{
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
    s_addr_in.sin_addr.s_addr = inet_addr("127.0.0.1");      //trans char * to in_addr_t
    s_addr_in.sin_family = AF_INET;
    s_addr_in.sin_port = htons(SOCK_PORT);

    client.sin_family = AF_INET;
    client.sin_addr.s_addr = htonl(INADDR_ANY);
    client.sin_port = htons(50000);
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

    while(1)
    {
        printf("Please input something you wanna say(input \"quit\" to quit):\n");
        gets(data_send);
        //scanf("%[^\n]",data_send);         //or you can also use this
        tempfd = write(sockfd,data_send,BUFFER_LENGTH);
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
    }

    int ret = shutdown(sockfd,SHUT_WR);       //or you can use func close()--<unistd.h> to close the fd
    assert(ret != -1);
    return 0;
}
