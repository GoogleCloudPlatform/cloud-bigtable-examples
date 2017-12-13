namespace HelloWorld
{
    public class HelloWorldSettings
    {
        public string InstanceId { get; set; }
        public string ProjectId { get; set; }
        public string TableName { get; set; }
        public string ColumnFamily { get; set; }
        public string RowKeyPrefix { get; set; }
        public string ColumnName { get; set; } 
        public string[] Greetings { get; set; }
    }
}